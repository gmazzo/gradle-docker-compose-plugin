package io.github.gmazzo.docker.compose

import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent

public class DockerComposeBasePlugin @Inject constructor(
    private val gradle: Gradle,
) : Plugin<ExtensionAware> {

    override fun apply(target: ExtensionAware) {
        when (target) {
            is Project -> target.configure()
            is Settings -> gradle.lifecycle.beforeProject { apply<DockerComposeBasePlugin>() }
            else -> error("Unsupported target type: $target")
        }
    }

    private fun Project.configure() {
        val dockerService = gradle.sharedServices.registerIfAbsent("docker", DockerService::class)
        val dockerExtension = dockerService.get()

        val extension: DockerComposeExtension = extensions.create(
            DockerComposeExtension::class,
            "dockerCompose",
            DockerComposeChildExtension::class,
            dockerExtension,
        )

        with(extension) {
            from(dockerExtension)
            projectName.convention(provider { dockerName })
            workingDirectory.convention(layout.projectDirectory)

            services.all spec@{
                val baseDir = layout.projectDirectory.dir("src/${this@spec.name}")

                this@spec.from(extension)

                this@spec.projectName
                    .convention(extension.projectName.map { "${it}_${this@spec.name.dockerName}" })
                    .finalizeValueOnRead()

                this@spec.composeFile(
                    baseDir.file("docker-compose.yml"),
                    baseDir.file("docker-compose.yaml"),
                    baseDir.file("docker-compose.json"),
                ).finalizeValueOnRead()

                buildService = gradle.sharedServices.registerIfAbsent("docker$path:${this@spec.name}", DockerComposeService::class) {
                    parameters params@{
                        this@params.serviceName.set(this@spec.name)
                        this@params.dockerService.set(dockerService)
                        this@params.from(this@spec)
                    }
                    maxParallelUsages.convention(1)
                }

                val taskSuffix = when (this@spec.name) {
                    SourceSet.MAIN_SOURCE_SET_NAME -> ""
                    else -> this@spec.name.replaceFirstChar { it.uppercase() }
                }

                tasks.register<DockerComposeInitTask>("init${taskSuffix}Containers") task@{
                    group = "Docker"
                    description = "Creates (but does not start) the containers of '$name' source set"

                    this@task.usesService(dockerService)
                    this@task.usesService(this@spec.buildService)
                    this@task.dockerService.set(dockerService)
                    this@task.dockerComposeService.set(this@spec.buildService)
                    this@task.from(this@spec)
                }
            }
        }
    }

    private fun DockerComposeSettings.from(source: DockerComposeSettings) {
        optionsCreate.convention(source.optionsCreate).finalizeValueOnRead()
        optionsUp.convention(source.optionsUp).finalizeValueOnRead()
        optionsDown.convention(source.optionsDown).finalizeValueOnRead()
        keepContainersRunning.convention(source.keepContainersRunning).finalizeValueOnRead()
        waitForTCPPorts.enabled.convention(source.waitForTCPPorts.enabled).finalizeValueOnRead()
        waitForTCPPorts.include.convention(source.waitForTCPPorts.include).finalizeValueOnRead()
        waitForTCPPorts.exclude.convention(source.waitForTCPPorts.exclude).finalizeValueOnRead()
        waitForTCPPorts.timeout.convention(source.waitForTCPPorts.timeout).finalizeValueOnRead()
        printPortMappings.convention(source.printPortMappings).finalizeValueOnRead()
        printLogs.convention(source.printLogs).finalizeValueOnRead()
        (this as DockerComposeCreateSettings).from(source)
    }

    private fun DockerComposeCreateSettings.from(source: DockerComposeCreateSettings) {
        projectName.convention(source.projectName).finalizeValueOnRead()
        workingDirectory.convention(source.workingDirectory).finalizeValueOnRead()
        composeFile.from(source.composeFile).finalizeValueOnRead()
        optionsCreate.convention(source.optionsCreate).finalizeValueOnRead()
    }

    private val Project.dockerName
        get() = generateSequence(project, Project::getParent)
            .map { it.name.dockerName }
            .joinToString(separator = "-")

    private val String.dockerName
        get() = lowercase().replace("\\W".toRegex(), "_")

}
