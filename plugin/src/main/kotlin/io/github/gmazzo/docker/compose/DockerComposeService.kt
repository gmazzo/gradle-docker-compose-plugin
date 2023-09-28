package io.github.gmazzo.docker.compose

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

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
        run()
    }

    val containers: List<DockerContainer>
        get() {
            if (!parameters.hasComposeFile) return emptyList()

            val content: String = with(ByteArrayOutputStream()) out@{
                docker.composeExec(parameters, "ps", "--format=json") { standardOutput = PrintStream(this@out) }
                toString(StandardCharsets.UTF_8)
            }
            try {
                return if (content.startsWith("[")) json.decodeFromString(content)
                else content.lineSequence()
                    .filter { it.isNotBlank() }
                    .map { json.decodeFromString<DockerContainer>(it) }
                    .toList()

            } catch (e: SerializationException) {
                throw IllegalArgumentException("Failed to parse JSON:\n$content\n", e)
            }
        }

    val containersAsSystemProperties: Map<String, String>
        get() = buildMap {
            containers.forEach {
                val containerName = it.name.replaceFirst(parameters.projectName.get(), name)

                it.publishers.forEach { p ->
                    put("container.$containerName.${p.protocol}${p.targetPort}", "${p.url}:${p.publishedPort}")
                }
            }
        }

    override fun run() {
        if (parameters.hasComposeFile) {
            logger.lifecycle("Starting containers of Docker service `{}`...", name)
            docker.composeExec(
                parameters, "up", "--wait",
                *(parameters.optionsCreate.get() + parameters.optionsUp.get()).toTypedArray()
            )

            if (parameters.printPortMappings.get()) {
                printPortMappings()
            }
            if (parameters.printLogs.get()) {
                startLogsThread()
            }
        }
    }

    override fun close() {
        if (parameters.hasComposeFile) {
            logger.lifecycle("Stopping containers of Docker service `{}`...", name)
            docker.composeExec(parameters, "down", *parameters.optionsDown.get().toTypedArray())
        }
    }

    private fun printPortMappings() {
        val header = "JVM System Property" to "Mapped Port"
        val rows = containersAsSystemProperties.toList()
        val size1 = (rows.asSequence() + header).maxOf { it.first.length }
        val size2 = (rows.asSequence() + header).maxOf { it.second.length }

        logger.lifecycle(rows.joinToString(
            prefix = "\n Containers ports of `$name` Docker service:\n" +
            "┌" + "─".repeat(size1 + 2) + "┬"+ "─".repeat(size2 + 2) + "┐\n" +
                    "│ " + header.first + " ".repeat(size1 - header.first.length ) + " │ " + header.second + " ".repeat(size2 - header.second.length ) + " │\n" +
                "├" + "─".repeat(size1 + 2) + "┼"+ "─".repeat(size2 + 2) + "┤\n",
            transform = { (col1, col2) -> "│ " + col1 + " ".repeat(size1 - col1.length ) + " │ " + col2 + " ".repeat(size2 - col2.length ) + " │\n" },
            separator = "",
            postfix = "└" + "─".repeat(size1 + 2) + "┴"+ "─".repeat(size2 + 2) + "┘\n"
        ))
    }

    private fun startLogsThread() = thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
        docker.composeExec(parameters, "logs", "--follow")
    }

    private val DockerComposeSource.hasComposeFile
        get() = !composeFile.asFileTree.isEmpty

    interface Params : BuildServiceParameters, DockerComposeSource {

        val serviceName: Property<String>

        val dockerService: Property<DockerService>

    }

}
