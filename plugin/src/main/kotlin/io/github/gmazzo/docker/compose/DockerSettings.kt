package io.github.gmazzo.docker.compose

import org.gradle.api.Action
import org.gradle.api.credentials.Credentials
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

@JvmDefaultWithoutCompatibility
interface DockerSettings : Credentials {

    /**
     * The `docker` command path.
     *
     * It should not be changed unless you want to provide a full path for it.
     *
     * This setting is shared between all [org.gradle.api.Project]s of the build
     */
    val command: Property<String>

    /**
     * Optional options that corresponds to `Global Options` of the `docker` command
     *
     * This setting is shared between all [org.gradle.api.Project]s of the build
     */
    val options: ListProperty<String>

    /**
     * Optional login information to perform `docker login` command before any [DockerComposeService] is created
     *
     * This setting is shared between all [org.gradle.api.Project]s of the build
     */
    @get:Nested
    val login: Login

    fun login(action: Action<Login>) = action.execute(login)

    interface Login {

        val server: Property<String>

        val username: Property<String>

        val password: Property<String>

    }

}
