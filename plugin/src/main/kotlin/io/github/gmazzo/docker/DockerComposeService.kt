package io.github.gmazzo.docker

import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class DockerComposeService @Inject constructor(
    private val execOperations: ExecOperations,
) : BuildService<DockerComposeService.Params>, AutoCloseable, Named {

    private val composeFile = parameters.spec.composeFile.singleFileOrThrowList()

    init {
        runCommand("up")
    }

    override fun close() {
        runCommand("down")
    }

    private fun runCommand(command: String) = execOperations.exec {
        workingDir = parameters.spec.workingDirectory.get().asFile
        commandLine = buildList {
            add(parameters.spec.command.get())
            addAll(parameters.spec.commandExtraArgs.get())
            add("-f")
            add(composeFile.absolutePath)
            add(command)
        }
    }

    private fun FileCollection.singleFileOrThrowList(): File = with(asFileTree) {
        try {
            return@with singleFile

        } catch (e: IllegalStateException) {
            error(files.joinToString(e.message.orEmpty()) { "\n\t- $it" })
        }
    }

    interface Params : BuildServiceParameters {

        var spec: DockerComposeSpec

    }

}
