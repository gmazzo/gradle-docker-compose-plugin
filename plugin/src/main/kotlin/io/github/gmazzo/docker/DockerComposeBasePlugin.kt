package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.api.tasks.SourceSet
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import javax.inject.Inject

class DockerComposeBasePlugin @Inject constructor(
    private val sharedServices: BuildServiceRegistry,
) : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension: DockerComposeExtension = extensions.create("dockerCompose")

        with(extension) {
            projectName.convention(
                if (rootProject == project) rootProject.name.dockerName
                else "${rootProject.name.dockerName}-${project.name.dockerName}"
            )
            command.convention("docker").finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            verbose.convention(true).finalizeValueOnRead()
        }

        val dockerService = sharedServices.registerIfAbsent("docker", DockerService::class) {
            parameters.command.set(extension.command)
            parameters.commandExtraArgs.set(extension.commandExtraArgs)
            parameters.setupDocker.set(extension.setupDocker)
            parameters.cleanupDocker.set(extension.cleanupDocker)
            parameters.verbose.set(extension.verbose)
        }

        extension.services.all spec@{
            val baseDir = layout.projectDirectory.dir("src/$name")

            projectName.convention(extension.projectName.map { "${it}_${name.dockerName}" }).finalizeValueOnRead()
            composeFile
                .from(
                    baseDir.file("docker-compose.yml"),
                    baseDir.file("docker-compose.yaml"),
                    baseDir.file("docker-compose.json"),
                )
                .finalizeValueOnRead()
            workingDirectory.convention(extension.workingDirectory).finalizeValueOnRead()
            verbose.convention(extension.verbose).finalizeValueOnRead()

            buildService = sharedServices.registerIfAbsent("dockerCompose${name.capitalized()}", DockerComposeService::class) {
                parameters params@{
                    this@params.serviceName.set(name)
                    this@params.dockerService.set(dockerService)
                    this@params.projectName.set(this@spec.projectName)
                    this@params.composeFile.setFrom(this@spec.composeFile)
                    this@params.workingDirectory.set(this@spec.workingDirectory)
                    this@params.verbose.set(this@spec.verbose)
                }
                maxParallelUsages.convention(1)
            }

            tasks.register<DockerComposeInitTask>("init${if (name == SourceSet.MAIN_SOURCE_SET_NAME) "" else name.capitalized()}Containers") {
                group = "Docker"
                description = "Creates (but does not start) the containers of '$name' source set"

                projectName.set(this@spec.projectName)
                composeFile.setFrom(this@spec.composeFile)
                workingDirectory.set(this@spec.workingDirectory)
                verbose.set(this@spec.verbose)
            }
        }
    }

    private val String.dockerName
        get() = lowercase().replace("\\W".toRegex(), "_")

}
