package io.github.gmazzo.docker

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface DockerComposeSettings {

    /**
     * The name of the Docker compose project used to generate container names.
     *
     * Maps to `--project-name` argument
     */
    val projectName: Property<String>

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
     * The working directory for the `docker-compose` command.
     *
     * Defaults to the project's directory.
     */
    val workingDirectory: DirectoryProperty

    /**
     * If logs from the running containers should be printed to the Gradle standard output or not
     */
    val verbose: Property<Boolean>

}
