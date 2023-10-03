package io.github.gmazzo.docker.compose

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("LeakingThis")
abstract class DockerComposeService : BuildService<DockerComposeService.Params>, AutoCloseable, Runnable {

    private val logger = Logging.getLogger(DockerComposeService::class.java)

    private val name = parameters.serviceName.get()

    private val docker = parameters.dockerService.get()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    init {
        try {
            run()

        } catch (e: Throwable) {
            logger.error("Failed to start containers of Docker service `{}`: {}", name, e.localizedMessage)
            runCatching { close() }
            throw e
        }
    }

    val containers: List<DockerContainer>
        get() {
            if (!parameters.hasComposeFile) return emptyList()

            val containerIds = docker.composeExec(parameters, "ps", "--all", "--quiet")
                .output
                .lineSequence()
                .filter { it.isNotBlank() }
                .toList()

            if (containerIds.isEmpty()) return emptyList()

            val content = docker.exec("inspect", *containerIds.toTypedArray()).output
            try {
                return json.decodeFromString<List<DockerContainer>>(content)

            } catch (e: SerializationException) {
                throw IllegalArgumentException("Failed to parse JSON:\n$content\n", e)
            }
        }

    val containersAsSystemProperties: Map<String, String>
        get() = buildMap {
            containers.forEach { container ->
                val containerName = container.name
                    .replaceFirst(parameters.projectName.get(), name)
                    .removePrefix("/")
                    .removeSuffix("-1")

                put("container.$containerName.host", container.host)

                container.networkSettings.ports.forEach { (name, bindings) ->
                    val (type, port) = name.typeAndPort

                    bindings?.firstOrNull()?.let {
                        put("container.$containerName.$type$port", it.hostPort.toString())
                    }
                }
            }
        }

    val containersAsEnvironmentVariables: Map<String, String>
        get() = containersAsSystemProperties.mapKeys { (key) -> key.envVarName }

    override fun run() {
        if (parameters.hasComposeFile) {
            logger.lifecycle("Starting containers of Docker service `{}`...", name)

            startContainers()
            if (parameters.waitForTCPPorts.enabled.get()) {
                waitForTCPPorts()
            }
            if (parameters.printPortMappings.get()) {
                printPortMappings()
            }
            if (parameters.printLogs.get()) {
                startLogsThread()
            }
        }
    }

    override fun close() {
        if (!parameters.keepContainersRunning.get() && parameters.hasComposeFile) {
            logger.lifecycle("Stopping containers of Docker service `{}`...", name)
            docker.composeExec(parameters, "down", *parameters.optionsDown.get().toTypedArray())
        }
    }

    private fun startContainers() {
        val result = docker.composeExec(
            parameters, "up",
            *(parameters.optionsCreate.get() + parameters.optionsUp.get()).toTypedArray(),
            failNonZeroExitValue = false
        )

        // print logs of failed cont
        val failed = containers.asSequence()
            .filter { !it.state.running && it.state.exitCode != 0 }
            .map { it.name.removePrefix("/") }
            .toList()
        if (failed.isNotEmpty()) {
            val logs = docker.exec("logs", *failed.toTypedArray(), failNonZeroExitValue = false).output

            logger.error(logs)
            logger.error(
                failed.joinToString(
                    prefix = "Containers ",
                    transform = { "`$it`" },
                    postfix = " are not running."
                )
            )
        }

        result.assertNormalExitValue()
    }

    private fun waitForTCPPorts() {
        val include = parameters.waitForTCPPorts.include.get().takeUnless { it.isEmpty() }
        val exclude = parameters.waitForTCPPorts.exclude.get().takeUnless { it.isEmpty() }

        val start = System.currentTimeMillis()
        logger.lifecycle("Waiting for TCP ports to become available...")
        val ports = containers.asSequence()
            .flatMap { container ->
                val containerHost = container.host

                container.networkSettings.ports.asSequence().flatMap { (name, bindings) ->
                    val (type, port) = name.typeAndPort

                    if (type.equals("tcp", ignoreCase = true)) bindings.orEmpty().asSequence().map {
                        Triple("${container.name.removePrefix("/")}:$type$port", containerHost, it.hostPort)
                    } else emptySequence()
                }
            }
            .filter { (name) -> include == null || name in include }
            .filter { (name) -> exclude == null || name !in exclude }
            .toMutableList()

        val endAt = start + parameters.waitForTCPPorts.timeout.get()
        var attempt = 0
        while (System.currentTimeMillis() < endAt && ports.isNotEmpty()) {
            attempt++
            Thread.sleep(100)

            val it = ports.iterator()

            while (it.hasNext()) {
                val (name, host, port) = it.next()

                logger.info("Trying to connect to `{}` -> `{}:{}` (attempt #{})...", name, host, port, attempt)
                runCatching {
                    Socket(host, port).use { socket ->
                        // port is open
                        logger.info("Port `{}` -> `{}:{}` is available.", name, host, port)
                        it.remove()
                    }
                }
            }
        }
        val duration = (System.currentTimeMillis() - start).toDuration(DurationUnit.MILLISECONDS)
        if (ports.isEmpty()) {
            logger.info("Ports became available after `{}`", duration)

        } else {
            logger.warn("Ports still not available after `{}`:\n{}",
                duration,
                ports.joinToString(separator = "") { (name, host, port) -> " - $name -> $host:$port\n" })
        }
    }

    private fun printPortMappings() {
        val header1 = "JVM System Property"
        val header2 = "Environment Variable"
        val header3 = "Value"
        val rows = containersAsSystemProperties.toList()
        val size1 = (rows.asSequence().map { it.first } + header1).maxOf { it.length }
        val size2 = (rows.asSequence().map { it.first } + header2).maxOf { it.length }
        val size3 = (rows.asSequence().map { it.second } + header3).maxOf { it.length }

        logger.lifecycle(
            rows.joinToString(
                prefix = "\nProperties of `$name` Docker service:\n" +
                        "┌" + "─".repeat(size1 + 2) + "┬" + "─".repeat(size2 + 2) + "┬" + "─".repeat(size3 + 2) + "┐\n" +
                        "│ " + header1 + " ".repeat(size1 - header1.length) + " │ " + header2 + " ".repeat(size2 - header2.length) + " │ " + header3 + " ".repeat(size3 - header3.length) + " │\n" +
                        "├" + "─".repeat(size1 + 2) + "┼" + "─".repeat(size2 + 2) + "┼" + "─".repeat(size3 + 2) + "┤\n",
                transform = { (name, value) ->
                    "│ " + name + " ".repeat(size1 - name.length) + " │ " + name.envVarName + " ".repeat(size2 - name.length) + " │ " + value + " ".repeat(size3 - value.length) + " │\n"
                },
                separator = "",
                postfix = "└" + "─".repeat(size1 + 2) + "┴" + "─".repeat(size2 + 2) + "┴" + "─".repeat(size3 + 2) + "┘\n"
            )
        )
    }

    private fun startLogsThread() = thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
        docker.composeExec(parameters, "logs", "--follow")
    }

    private val DockerComposeCreateSettings.hasComposeFile: Boolean
        get() = !composeFile.asFileTree.isEmpty

    private val String.envVarName
        get() = replace("(\\p{javaLowerCase}+)|(\\W+)".toRegex()) { m ->
            m.groups[1]?.value?.uppercase() ?: "_".repeat(m.groupValues[2].length)
        }

    private val String.typeAndPort: Pair<String, Int>
        get() = checkNotNull("(\\d+)/(.*)".toRegex().matchEntire(this)) {
            "Port name '$this' does not have the format <number>/<type>"
        }.let { it.groupValues[2] to it.groupValues[1].toInt() }

    private val DockerContainer.host: String
        get() = networkSettings.ipAddress.takeIf { it.isNotBlank() } ?: InetAddress.getByName(null).hostAddress

    interface Params : BuildServiceParameters, DockerComposeSettings {

        val serviceName: Property<String>

        val dockerService: Property<DockerService>

    }

}
