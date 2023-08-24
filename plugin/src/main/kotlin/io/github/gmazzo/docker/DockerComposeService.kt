package io.github.gmazzo.docker

import io.github.gmazzo.docker.data.DockerContainerInfo
import kotlinx.serialization.json.Json
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
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

    private val composeFile
        get() = parameters.composeFile.singleFileOrThrowList()

    init {
        run()
    }

    override fun run() {
        if (composeFile != null) {
            println("Starting containers of Docker service `$name`...")
            runCommand("up", "--wait")

            if (parameters.printLogs.get()) {
                thread(isDaemon = true, name = "DockerCompose log for service `$name`") {
                    runCommand("logs", "--follow")
                }
            }
        }
    }

    override fun close() {
        if (composeFile != null) {
            println("Stopping containers of Docker service `$name`...")
            runCommand("down")
        }
    }

    val containersInfo: List<DockerContainerInfo>
        get() {
            if (composeFile == null) return emptyList()

            val json = ByteArrayOutputStream()
            runCommand("ps", "--format=json", output = PrintStream(json))
            return Json.decodeFromString(json.toString(StandardCharsets.UTF_8))
        }

    val containersPortsAsSystemProperties
        get(): Map<String, String> = buildMap {
            containersInfo.forEach {
                it.publishers.forEach { p ->
                    put("container.${it.name}.${p.protocol}${p.targetPort}", "${p.url}:${p.publishedPort}")
                }
            }
        }

    private fun runCommand(vararg commands: String, output: PrintStream = System.out) = execOperations.exec {
        workingDir = parameters.workingDirectory.get().asFile
        commandLine = buildList {
            add(parameters.command.get())
            addAll(parameters.commandExtraArgs.get())
            add("-f")
            add(composeFile!!.absolutePath)
            addAll(commands)
        }
        standardOutput = output
    }

    private fun FileCollection.singleFileOrThrowList(): File? = with(asFileTree) {
        if (isEmpty) return@with null
        try {
            return@with singleFile

        } catch (e: IllegalStateException) {
            error(files.joinToString(e.message.orEmpty()) { "\n\t- $it" })
        }
    }

    interface Params : BuildServiceParameters, DockerComposeSettings {

        val name: Property<String>

        val composeFile: ConfigurableFileCollection

    }

}
