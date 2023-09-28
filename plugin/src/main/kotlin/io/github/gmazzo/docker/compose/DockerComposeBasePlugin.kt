package io.github.gmazzo.docker.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.api.tasks.SourceSet
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DockerComposeBasePlugin @Inject constructor(
    private val sharedServices: BuildServiceRegistry,
) : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension: DockerComposeExtension = extensions.create("dockerCompose")
        val rootExtension: DockerComposeExtension? =
            if (project != rootProject) rootProject.extensions.findByType()
            else null

        with(extension) {
            // DockerSettings defaults
            command.convention("docker").finalizeValueOnRead()
            options.finalizeValueOnRead()
            keepContainersRunning.convention(false).finalizeValueOnRead()
            printPortMappings.convention(true).finalizeValueOnRead()
            printLogs.convention(true).finalizeValueOnRead()
            waitForTCPPorts.enabled.convention(true).finalizeValueOnRead()
            waitForTCPPorts.include.finalizeValueOnRead()
            waitForTCPPorts.exclude.finalizeValueOnRead()
            waitForTCPPorts.timeout.convention(TimeUnit.MINUTES.toMillis(1).toInt()).finalizeValueOnRead()
            login.server.finalizeValueOnRead()
            login.username.finalizeValueOnRead()
            login.password.finalizeValueOnRead()

            if (rootExtension != null) {
                command.convention(rootExtension.command)
                options.convention(rootExtension.options)
                keepContainersRunning.convention(rootExtension.keepContainersRunning)
                waitForTCPPorts.enabled.convention(rootExtension.waitForTCPPorts.enabled)
                waitForTCPPorts.include.convention(rootExtension.waitForTCPPorts.include)
                waitForTCPPorts.exclude.convention(rootExtension.waitForTCPPorts.exclude)
                waitForTCPPorts.timeout.convention(rootExtension.waitForTCPPorts.timeout)
                printPortMappings.convention(rootExtension.printPortMappings)
                printLogs.convention(rootExtension.printLogs)
                login.server.convention(rootExtension.login.server)
                login.username.convention(rootExtension.login.username)
                login.password.convention(rootExtension.login.password)
            }

            // DockerComposeSettings defaults
            projectName.convention(
                if (rootProject == project) rootProject.name.dockerName
                else "${rootProject.name.dockerName}-${project.name.dockerName}"
            ).finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            optionsCreate.apply { add("--remove-orphans") }.finalizeValueOnRead()
            optionsUp.finalizeValueOnRead()
            optionsDown.finalizeValueOnRead()

            if (rootExtension != null) {
                optionsCreate.convention(rootExtension.optionsCreate)
                optionsUp.convention(rootExtension.optionsUp)
                optionsDown.convention(rootExtension.optionsDown)
            }
        }

        val dockerService = sharedServices.registerIfAbsent("docker$path", DockerService::class) {
            parameters.command.set(extension.command)
            parameters.options.set(extension.options)
            parameters.login.server.set(extension.login.server)
            parameters.login.username.set(extension.login.username)
            parameters.login.password.set(extension.login.password)
        }

        extension.services.all spec@{
            val baseDir = layout.projectDirectory.dir("src/$name")

            projectName.convention(extension.projectName.map { "${it}_${name.dockerName}" }).finalizeValueOnRead()
            composeFile(
                    baseDir.file("docker-compose.yml"),
                    baseDir.file("docker-compose.yaml"),
                    baseDir.file("docker-compose.json"),
                ).finalizeValueOnRead()
            workingDirectory.convention(extension.workingDirectory).finalizeValueOnRead()
            optionsCreate.convention(extension.optionsCreate).finalizeValueOnRead()
            optionsUp.convention(extension.optionsUp).finalizeValueOnRead()
            optionsDown.convention(extension.optionsDown).finalizeValueOnRead()
            keepContainersRunning.convention(extension.keepContainersRunning).finalizeValueOnRead()
            waitForTCPPorts.enabled.convention(extension.waitForTCPPorts.enabled).finalizeValueOnRead()
            waitForTCPPorts.include.convention(extension.waitForTCPPorts.include).finalizeValueOnRead()
            waitForTCPPorts.exclude.convention(extension.waitForTCPPorts.exclude).finalizeValueOnRead()
            waitForTCPPorts.timeout.convention(extension.waitForTCPPorts.timeout).finalizeValueOnRead()
            printPortMappings.convention(extension.printPortMappings).finalizeValueOnRead()
            printLogs.convention(extension.printLogs).finalizeValueOnRead()

            buildService = sharedServices.registerIfAbsent("docker$path:$name", DockerComposeService::class) {
                parameters params@{
                    this@params.serviceName.set(name)
                    this@params.dockerService.set(dockerService)
                    this@params.from(this@spec)
                }
                maxParallelUsages.convention(1)
            }

            tasks.register<DockerComposeInitTask>("init${if (name == SourceSet.MAIN_SOURCE_SET_NAME) "" else name.capitalized()}Containers") task@{
                group = "Docker"
                description = "Creates (but does not start) the containers of '$name' source set"

                this@task.usesService(dockerService)
                this@task.dockerService.set(dockerService)
                this@task.usesService(this@spec.buildService)
                this@task.dockerComposeService.set(this@spec.buildService)
                this@task.from(this@spec)
            }
        }
    }

    private fun DockerComposeCreateSettings.from(source: DockerComposeCreateSettings) {
        projectName.set(source.projectName)
        workingDirectory.set(source.workingDirectory)
        composeFile.setFrom(source.composeFile)
        optionsCreate.set(source.optionsCreate)
    }

    private fun DockerComposeSettings.from(source: DockerComposeSettings) {
        (this as DockerComposeCreateSettings).from(source)
        optionsUp.set(source.optionsUp)
        optionsDown.set(source.optionsDown)
        keepContainersRunning.set(source.keepContainersRunning)
        waitForTCPPorts.enabled.set(source.waitForTCPPorts.enabled)
        waitForTCPPorts.include.set(source.waitForTCPPorts.include)
        waitForTCPPorts.exclude.set(source.waitForTCPPorts.exclude)
        waitForTCPPorts.timeout.set(source.waitForTCPPorts.timeout)
        printPortMappings.set(source.printPortMappings)
        printLogs.set(source.printLogs)
    }

    private val String.dockerName
        get() = lowercase().replace("\\W".toRegex(), "_")

}
