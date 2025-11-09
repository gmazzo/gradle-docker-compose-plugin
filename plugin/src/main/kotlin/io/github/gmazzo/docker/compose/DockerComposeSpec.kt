package io.github.gmazzo.docker.compose

import java.io.Serializable
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.JavaForkOptions
import org.gradle.process.ProcessForkOptions

public abstract class DockerComposeSpec : Named, DockerComposeSettings {

    /**
     * Returns the service reference to be used on the [Task.usesService] API
     */
    public lateinit var buildService: Provider<DockerComposeService>
        internal set

    /**
     * Binds [buildService] to the given [task] as a [BuildService] and register the `docker-compose` file as [org.gradle.api.tasks.TaskInputs.files]
     *
     * Optionally, if [task] supports [JavaForkOptions] also exposes its [DockerComposeService.containers] as [JavaForkOptions.systemProperties]
     */
    public fun bindTo(task: TaskProvider<*>): Unit =
        task.configure(::bindTo)

    /**
     * Binds [buildService] to the given [task] as a [BuildService] and register the `docker-compose` file as [org.gradle.api.tasks.TaskInputs.files]
     *
     * Optionally, if [task] supports [JavaForkOptions] also exposes its [DockerComposeService.containers] as [JavaForkOptions.systemProperties]
     */
    public fun bindTo(task: Task): Unit = with(task) {
        usesService(buildService)

        inputs.files(composeFile)
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .optional()

        if (task is JavaForkOptions) {
            doFirst(AddSystemPropertiesAction(buildService))
        }
        if (task is ProcessForkOptions) {
            doFirst(AddEnvironmentAction(buildService))
        }
    }

    private class AddSystemPropertiesAction(
        private val buildService: Provider<DockerComposeService>,
    ) : Action<Task>, Serializable {
        override fun execute(task: Task) {
            (task as JavaForkOptions).systemProperties.putAll(buildService.get().containersAsSystemProperties)
        }
    }

    private class AddEnvironmentAction(
        private val buildService: Provider<DockerComposeService>,
    ) : Action<Task>, Serializable {
        override fun execute(task: Task) {
            (task as ProcessForkOptions).environment.putAll(buildService.get().containersAsEnvironmentVariables)
        }
    }

}
