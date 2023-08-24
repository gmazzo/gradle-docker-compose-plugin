package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.registerIfAbsent

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
            composeFile.finalizeValueOnRead()
            workingDirectory.convention(extension.workingDirectory).finalizeValueOnRead()
            exclusive.convention(true).finalizeValueOnRead()
            printLogs.convention(extension.printLogs).finalizeValueOnRead()

            service = gradle.sharedServices.registerIfAbsent(name, DockerComposeService::class) param@{
                parameters {
                    name.set(this@spec.name)
                    command.set(this@spec.command)
                    commandExtraArgs.set(this@spec.commandExtraArgs)
                    composeFile.setFrom(this@spec.composeFile)
                    workingDirectory.set(this@spec.workingDirectory)
                    printLogs.set(this@spec.printLogs)
                }

                if (this@spec.exclusive.get()) {
                    maxParallelUsages.set(1)
                }
            }
        }
    }

}
