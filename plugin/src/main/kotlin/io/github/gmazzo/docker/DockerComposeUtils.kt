package io.github.gmazzo.docker

import org.gradle.api.file.FileCollection
import org.gradle.process.ExecOperations
import java.io.File
import java.io.PrintStream

internal fun ExecOperations.dockerCompose(
    spec: DockerComposeSource,
    vararg commands: String,
    output: PrintStream = System.out,
) = exec {
    workingDir = spec.workingDirectory.get().asFile
    commandLine = buildList {
        add(spec.command.get())
        add("--project-name")
        add(spec.projectName.get())
        add("-f")
        add(spec.composeFile.singleFileOrThrow.absolutePath)
        addAll(spec.commandExtraArgs.get())
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

internal val DockerComposeSource.hasComposeFile
    get() = !composeFile.asFileTree.isEmpty

internal fun DockerComposeSource.copyFrom(other: DockerComposeSource) {
    copyFrom(other as DockerComposeSettings)
    composeFile.setFrom(other.composeFile)
}

internal fun DockerComposeSettings.copyFrom(other: DockerComposeSettings) {
    projectName.set(other.projectName)
    command.set(other.command)
    commandExtraArgs.set(other.commandExtraArgs)
    workingDirectory.set(other.workingDirectory)
    verbose.set(other.verbose)
}

val String.dockerName
    get() = lowercase().replace("\\W".toRegex(), "_")
