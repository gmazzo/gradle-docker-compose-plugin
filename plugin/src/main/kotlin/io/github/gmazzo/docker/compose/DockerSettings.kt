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
     */
    val command: Property<String>

    /**
     * Any extra argument to append after all `docker compose up` and similar commands.
     */
    val commandExtraArgs: ListProperty<String>

    /**
     * If logs from the running containers should be printed to the Gradle standard output or not
     */
    val verbose: Property<Boolean>

    /**
     * Optional login information to perform `docker login` command before any [DockerComposeService] is created
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
