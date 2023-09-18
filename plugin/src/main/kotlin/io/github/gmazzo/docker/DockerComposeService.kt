package io.github.gmazzo.docker

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

    private val name = parameters.name.get()

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

            val content = ByteArrayOutputStream()
            execOperations.dockerCompose(parameters, "ps", "--format=json", output = PrintStream(content))
            return json.decodeFromString(content.toString(StandardCharsets.UTF_8))
        }

    val containersAsSystemProperties: Map<String, String>
        get() = buildMap {
            containers.forEach {
                it.publishers.forEach { p ->
                    put("container.${it.name}.${p.protocol}${p.targetPort}", "${p.url}:${p.publishedPort}")
                }
            }
        }

    override fun run() {
        if (parameters.hasComposeFile) {
            println("Starting containers of Docker service `$name`...")
            execOperations.dockerCompose(parameters, "up", "--remove-orphans", "--wait")

            if (parameters.printLogs.get()) {
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

    interface Params : BuildServiceParameters, DockerComposeSettings {

        val name: Property<String>

    }

}
