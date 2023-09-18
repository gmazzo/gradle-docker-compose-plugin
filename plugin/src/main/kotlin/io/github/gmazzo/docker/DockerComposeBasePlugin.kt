package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class DockerComposeBasePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension: DockerComposeExtension = extensions.create("dockerCompose")

        with(extension) {
            command.convention("docker-compose").finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            printLogs.convention(true).finalizeValueOnRead()
        }

        extension.services.all spec@{
            val baseDir = layout.projectDirectory.dir("src/$name")

            command.convention(extension.command).finalizeValueOnRead()
            commandExtraArgs.convention(extension.commandExtraArgs).finalizeValueOnRead()
            composeFile
                .from(
                    baseDir.file("docker-compose.yml"),
                    baseDir.file("docker-compose.yaml"),
                    baseDir.file("docker-compose.json"),
                )
                .finalizeValueOnRead()
            workingDirectory.convention(extension.workingDirectory).finalizeValueOnRead()
            printLogs.convention(extension.printLogs).finalizeValueOnRead()

            tasks.register<DockerComposeInitTask>("init${if (name == SourceSet.MAIN_SOURCE_SET_NAME) "" else name.capitalized()}Containers") {
                group = "Docker"
                description = "Creates (but does not start) the containers of '$name' source set"
                this@spec.copyTo(this)
            }
        }
    }

}
