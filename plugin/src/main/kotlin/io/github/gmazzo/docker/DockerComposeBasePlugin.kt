package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class DockerComposeBasePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension: DockerComposeExtension = extensions.create("dockerCompose")

        with(extension) {
            command.convention("docker-compose").finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            printLogs.convention(true).finalizeValueOnRead()
        }

        extension.services.all spec@{
            command.convention(extension.command).finalizeValueOnRead()
            commandExtraArgs.convention(extension.commandExtraArgs).finalizeValueOnRead()
            composeFile
                .from(layout.projectDirectory.dir("src/$name").asFileTree.matching {
                    include("docker-compose.{yml,yaml,json}")
                })
                .finalizeValueOnRead()
            workingDirectory.convention(extension.workingDirectory).finalizeValueOnRead()
            printLogs.convention(extension.printLogs).finalizeValueOnRead()
        }
    }

}
