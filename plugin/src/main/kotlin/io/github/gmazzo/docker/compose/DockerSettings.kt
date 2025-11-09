package io.github.gmazzo.docker.compose

import org.gradle.api.Action
import org.gradle.api.credentials.Credentials
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

public interface DockerSettings : Credentials {

    /**
     * The `docker` command path.
     *
     * It should not be changed unless you want to provide a full path for it.
     *
     * This setting is shared between all [org.gradle.api.Project]s of the build
     */
    public val command: Property<String>

    /**
     * Optional options that corresponds to `Global Options` of the `docker` command
     *
     * This setting is shared between all [org.gradle.api.Project]s of the build
     */
    public val options: ListProperty<String>

    /**
     * Optional login information to perform `docker login` command before any [DockerComposeService] is created
     *
     * This setting is shared between all [org.gradle.api.Project]s of the build
     */
    @get:Nested
    public val login: Login

    public fun login(action: Action<Login>): Unit = action.execute(login)

    public interface Login {

        public val server: Property<String>

        public val username: Property<String>

        public val password: Property<String>

    }

}
