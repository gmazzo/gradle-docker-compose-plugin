package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.registerIfAbsent

class DockerComposeBasePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension: DockerComposeExtension = extensions.create("dockerCompose")

        extension.services.all spec@{
            command.convention("docker-compose").finalizeValueOnRead()
            commandExtraArgs.finalizeValueOnRead()
            composeFile.finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            exclusive.convention(true).finalizeValueOnRead()
            printLogs.convention(true).finalizeValueOnRead()

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
