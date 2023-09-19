package io.github.gmazzo.docker

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.concurrent.thread

@Suppress("LeakingThis")
abstract class DockerComposeService @Inject constructor(
    private val execOperations: ExecOperations,
) : BuildService<DockerComposeService.Params>, AutoCloseable, Runnable {

    private val name = parameters.serviceName.get()

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

            val content: String = with(ByteArrayOutputStream()) {
                execOperations.dockerCompose(parameters, "ps", "--format=json", output = PrintStream(this))
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
            println("Starting containers of Docker service `$name`...")
            execOperations.dockerCompose(parameters, "up", "--remove-orphans", "--wait")

            if (parameters.verbose.get()) {
                containersAsSystemProperties.takeUnless { it.isEmpty() }?.entries?.joinToString(
                    prefix = "Containers ports are available trough properties:",
                    transform = { (key, value) -> "\n - $key -> $value" }
                )?.let(::println)

                thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
                    execOperations.dockerCompose(parameters, "logs", "--follow")
                }
            }
        }
    }

    override fun close() {
        if (parameters.hasComposeFile) {
            println("Stopping containers of Docker service `$name`...")
            execOperations.dockerCompose(parameters, "down")
        }
    }

    interface Params : BuildServiceParameters, DockerComposeSource {

        val serviceName: Property<String>

    }

}
