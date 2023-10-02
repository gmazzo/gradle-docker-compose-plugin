package io.github.gmazzo.docker.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.api.tasks.SourceSet
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.the
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DockerComposeBasePlugin @Inject constructor(
    private val sharedServices: BuildServiceRegistry,
) : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<DockerComposeBasePlugin>()

        val rootExtension: DockerComposeExtension? = if (project != rootProject) rootProject.the() else null
        val extension: DockerComposeExtension =
            if (rootExtension == null) extensions.create("dockerCompose")
            else extensions.create(DockerComposeExtension::class, "dockerCompose", DockerComposeChildExtension::class, rootExtension)

        with(extension) {
            // DockerSettings defaults
            command.convention("docker").finalizeValueOnRead()
            options.finalizeValueOnRead()
            login.server.finalizeValueOnRead()
            login.username.finalizeValueOnRead()
            login.password.finalizeValueOnRead()

            // DockerComposeSettings shared defaults
            optionsCreate.apply { add("--remove-orphans") }.finalizeValueOnRead()
            optionsUp.apply { add("--wait") }.finalizeValueOnRead()
            optionsDown.finalizeValueOnRead()
            keepContainersRunning.convention(false).finalizeValueOnRead()
            waitForTCPPorts.enabled.convention(true).finalizeValueOnRead()
            waitForTCPPorts.timeout.convention(TimeUnit.MINUTES.toMillis(1).toInt()).finalizeValueOnRead()
            printPortMappings.convention(true).finalizeValueOnRead()
            printLogs.convention(true).finalizeValueOnRead()
            if (rootExtension != null) {
                optionsCreate.convention(rootExtension.optionsCreate)
                optionsUp.convention(rootExtension.optionsUp)
                optionsDown.convention(rootExtension.optionsDown)
                keepContainersRunning.convention(rootExtension.keepContainersRunning)
                waitForTCPPorts.enabled.convention(rootExtension.waitForTCPPorts.enabled)
                waitForTCPPorts.timeout.convention(rootExtension.waitForTCPPorts.timeout)
                printPortMappings.convention(rootExtension.printPortMappings)
                printLogs.convention(rootExtension.printLogs)
            }

            // DockerComposeSettings exclusive defaults (not inherited from root)
            projectName.convention(provider { dockerName }).finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            waitForTCPPorts.include.finalizeValueOnRead()
            waitForTCPPorts.exclude.finalizeValueOnRead()
        }

        val dockerService = sharedServices.registerIfAbsent("docker", DockerService::class) {
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

    private val Project.dockerName
        get() = generateSequence(project, Project::getParent)
            .map { it.name.dockerName }
            .joinToString(separator = "-")

    private val String.dockerName
        get() = lowercase().replace("\\W".toRegex(), "_")

}
