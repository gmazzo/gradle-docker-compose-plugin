package io.github.gmazzo.docker.compose

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

@Suppress("LeakingThis")
abstract class DockerComposeService : BuildService<DockerComposeService.Params>, AutoCloseable, Runnable {

    private val logger = LoggerFactory.getLogger(DockerComposeService::class.java)

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
                docker.dockerComposeExec(parameters, "ps", "--format=json") { standardOutput = PrintStream(this@out) }
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
            logger.info("Starting containers of Docker service `{}`...", name)
            docker.dockerComposeExec(parameters, "up", "--remove-orphans", "--wait")

            if (parameters.verbose.get()) {
                containersAsSystemProperties.takeUnless { it.isEmpty() }?.entries?.joinToString(
                    prefix = "Containers ports are available trough properties:",
                    transform = { (key, value) -> "\n - $key -> $value" }
                )?.let(logger::info)

                thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
                    docker.dockerComposeExec(parameters, "logs", "--follow")
                }
            }
        }
    }

    override fun close() {
        if (parameters.hasComposeFile) {
            logger.info("Stopping containers of Docker service `{}`...", name)
            docker.dockerComposeExec(parameters, "down")
        }
    }

    private val DockerComposeSource.hasComposeFile
        get() = !composeFile.asFileTree.isEmpty

    interface Params : BuildServiceParameters, DockerComposeSource {

        val serviceName: Property<String>

        val dockerService: Property<DockerService>

    }

}
