package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.registerIfAbsent

class DockerComposeBasePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        //objects.namedDomainObjectSet(Provider::class)
        val extension: DockerComposeExtension = extensions.create("dockerCompose")

        extension.services.all spec@{
            command.convention("docker-compose").finalizeValueOnRead()
            commandExtraArgs.finalizeValueOnRead()
            composeFile.finalizeValueOnRead()
            workingDirectory.convention(layout.projectDirectory).finalizeValueOnRead()
            exclusive.convention(true).finalizeValueOnRead()

            service = gradle.sharedServices.registerIfAbsent(name, DockerComposeService::class) param@{
                parameters.spec = this@spec
                if (this@spec.exclusive.get()) {
                    maxParallelUsages.set(1)
                }
            }
        }
    }

}
