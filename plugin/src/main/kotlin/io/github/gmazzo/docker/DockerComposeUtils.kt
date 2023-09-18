package io.github.gmazzo.docker

import org.gradle.api.file.FileCollection
import org.gradle.process.ExecOperations
import java.io.File
import java.io.PrintStream

internal fun ExecOperations.dockerCompose(
    settings: DockerComposeSettings,
    vararg commands: String,
    output: PrintStream = System.out,
) = exec {
    workingDir = settings.workingDirectory.get().asFile
    commandLine = buildList {
        add(settings.command.get())
        add("--project-name")
        add(settings.projectName.get())
        add("-f")
        add(settings.composeFile.singleFileOrThrow.absolutePath)
        addAll(settings.commandExtraArgs.get())
        addAll(commands)
    }
    standardOutput = output
}

internal val FileCollection.singleFileOrThrow: File
    get() = with(asFileTree) {
        check(!isEmpty) {
            this@singleFileOrThrow.joinToString(
                prefix = "No `docker-compose` files found at:",
                separator = ""
            ) { "\n - $it" }
        }
        try {
            return singleFile

        } catch (e: IllegalStateException) {
            error(files.joinToString(e.message.orEmpty()) { "\n - $it" })
        }
    }

internal val DockerComposeSettings.hasComposeFile
    get() = !composeFile.asFileTree.isEmpty

internal fun DockerComposeSettings.copyFrom(other: DockerComposeSettings) {
    projectName.set(other.projectName)
    command.set(other.command)
    commandExtraArgs.set(other.commandExtraArgs)
    composeFile.setFrom(other.composeFile)
    workingDirectory.set(other.workingDirectory)
    printLogs.set(other.printLogs)
}

val String.dockerName
    get() = lowercase().replace("\\W".toRegex(), "_")
