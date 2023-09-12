package io.github.gmazzo.docker

import kotlinx.serialization.json.Json
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
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

    private val composeFile: File? = with(parameters.composeFile.asFileTree) {
        if (isEmpty) return@with null
        try {
            return@with singleFile

        } catch (e: IllegalStateException) {
            error(files.joinToString(e.message.orEmpty()) { "\n\t- $it" })
        }
    }

    init {
        run()
    }

    val containers: List<DockerContainer>
        get() = composeFile?.let { file ->
            val content = ByteArrayOutputStream()
            dockerCompose(file, "ps", "--format=json", output = PrintStream(content))
            return json.decodeFromString(content.toString(StandardCharsets.UTF_8))
        } ?: emptyList()

    val containersAsSystemProperties: Map<String, String>
        get() = buildMap {
            containers.forEach {
                it.publishers.forEach { p ->
                    put("container.${it.name}.${p.protocol}${p.targetPort}", "${p.url}:${p.publishedPort}")
                }
            }
        }

    override fun run() {
        composeFile?.let { file ->
            println("Starting containers of Docker service `$name`...")
            dockerCompose(file, "up", "--remove-orphans", "--wait")

            if (parameters.printLogs.get()) {
                thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
                    dockerCompose(file, "logs", "--follow")
                }
            }
        }
    }

    override fun close() {
        composeFile?.let { file ->
            println("Stopping containers of Docker service `$name`...")
            dockerCompose(file, "down")
        }
    }

    private fun dockerCompose(
        composeFile: File,
        vararg commands: String,
        output: PrintStream = System.out,
    ) = execOperations.exec {
        workingDir = parameters.workingDirectory.get().asFile
        commandLine = buildList {
            add(parameters.command.get())
            addAll(parameters.commandExtraArgs.get())
            add("-f")
            add(composeFile.absolutePath)
            addAll(commands)
        }
        standardOutput = output
    }

    interface Params : BuildServiceParameters, DockerComposeSettings {

        val name: Property<String>

    }

}
