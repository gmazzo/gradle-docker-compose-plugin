package io.github.gmazzo.docker.compose

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
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

            val containerIds = with(ByteArrayOutputStream()) out@{
                docker.composeExec(parameters, "ps", "--all", "--quiet") { standardOutput = PrintStream(this@out) }
                toString(StandardCharsets.UTF_8)
            }.lineSequence().filter { it.isNotBlank() }.toList()

            if (containerIds.isEmpty()) return emptyList()

            val content = with(ByteArrayOutputStream()) out@{
                docker.exec("inspect", *containerIds.toTypedArray()) { standardOutput = PrintStream(this@out) }
                toString(StandardCharsets.UTF_8)
            }
            try {
                return json.decodeFromString<List<DockerContainer>>(content)

            } catch (e: SerializationException) {
                throw IllegalArgumentException("Failed to parse JSON:\n$content\n", e)
            }
        }

    val containersAsSystemProperties: Map<String, String>
        get() = buildMap {
            containers.forEach { container ->
                val containerHost = container.host
                val containerName = container.name
                    .replaceFirst(parameters.projectName.get(), name)
                    .removePrefix("/")

                container.networkSettings.ports.forEach { (name, bindings) ->
                    val (type, port) = name.typeAndPort

                    bindings?.firstOrNull()?.let {
                        put("container.$containerName.$type$port", "$containerHost:${it.hostPort}")
                    }
                }
            }
        }

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
        val result = runCatching {
            docker.composeExec(
                parameters, "up", "--wait",
                *(parameters.optionsCreate.get() + parameters.optionsUp.get()).toTypedArray()
            )
        }

        val failed = containers.asSequence()
            .filter { !it.state.running && it.state.exitCode != 0 }
            .map { it.name.removePrefix("/") }
            .toList()

        if (failed.isNotEmpty()) {
            with(ByteArrayOutputStream()) out@{
                docker.exec("logs", *failed.toTypedArray()) { standardOutput = this@out }
                logger.error(toString(StandardCharsets.UTF_8))
            }
            throw IllegalStateException("Containers ${failed.joinToString { "`$it`" }} are not running.", result.exceptionOrNull())
        }
        result.getOrThrow()
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
        val header = "JVM System Property" to "Mapped Port"
        val rows = containersAsSystemProperties.toList()
        val size1 = (rows.asSequence() + header).maxOf { it.first.length }
        val size2 = (rows.asSequence() + header).maxOf { it.second.length }

        logger.lifecycle(
            rows.joinToString(
                prefix = "\nContainers ports of `$name` Docker service:\n" +
                        "┌" + "─".repeat(size1 + 2) + "┬" + "─".repeat(size2 + 2) + "┐\n" +
                        "│ " + header.first + " ".repeat(size1 - header.first.length) + " │ " + header.second + " ".repeat(
                    size2 - header.second.length
                ) + " │\n" +
                        "├" + "─".repeat(size1 + 2) + "┼" + "─".repeat(size2 + 2) + "┤\n",
                transform = { (col1, col2) ->
                    "│ " + col1 + " ".repeat(size1 - col1.length) + " │ " + col2 + " ".repeat(
                        size2 - col2.length
                    ) + " │\n"
                },
                separator = "",
                postfix = "└" + "─".repeat(size1 + 2) + "┴" + "─".repeat(size2 + 2) + "┘\n"
            )
        )
    }

    private fun startLogsThread() = thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
        docker.composeExec(parameters, "logs", "--follow")
    }

    private val DockerComposeCreateSettings.hasComposeFile: Boolean
        get() = !composeFile.asFileTree.isEmpty

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
