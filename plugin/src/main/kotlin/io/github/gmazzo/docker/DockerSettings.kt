package io.github.gmazzo.docker

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

@JvmDefaultWithoutCompatibility
interface DockerSettings {

    /**
     * The `docker` command path.
     *
     * It should not be changed unless you want to provide a full path for it.
     */
    val command: Property<String>

    /**
     * Any extra argument to append after all `docker compose up` and similar commands.
     */
    val commandExtraArgs: ListProperty<String>

    /**
     * Actions to be executed before any [DockerService] is started (such as a `docker login`)
     */
    val setupDocker: ListProperty<Action<DockerService>>

    /**
     * Adds an action to be executed before any [DockerService] is started (such as a `docker login`)
     */
    fun setupDocker(action: Action<DockerService>) = setupDocker.add(action)

    /**
     * Actions to be executed after all [DockerService] are stopped.
     */
    val cleanupDocker: ListProperty<Action<DockerService>>

    /**
     * Adds an action to be executed after all [DockerService] are stopped.
     */
    fun cleanupDocker(action: Action<DockerService>) = cleanupDocker.add(action)

    /**
     * If logs from the running containers should be printed to the Gradle standard output or not
     */
    val verbose: Property<Boolean>

}
