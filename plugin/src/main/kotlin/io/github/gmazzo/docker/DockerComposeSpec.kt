package io.github.gmazzo.docker

import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.process.JavaForkOptions
import javax.inject.Inject

abstract class DockerComposeSpec @Inject constructor(
    name: String,
    sharedServices: BuildServiceRegistry,
) : Named by (Named { name }), DockerComposeSettings {

    val buildService: Provider<DockerComposeService> =
        sharedServices.registerIfAbsent(name, DockerComposeService::class) spec@{
            parameters params@{
                this@params.name.set(name)
                this@DockerComposeSpec.copyTo(this@params)
            }
            this@spec.maxParallelUsages.convention(1)
        }

    fun bindTo(task: TaskProvider<*>) =
        task.configure(::bindTo)

    fun bindTo(task: Task) = with(task) {
        usesService(buildService)

        inputs.files(composeFile)
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .optional()

        if (task is JavaForkOptions) {
            doFirst { task.systemProperties.putAll(buildService.get().containersAsSystemProperties) }
        }
    }

}
