package io.github.gmazzo.docker.compose

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface DockerComposeCreateSettings {

    /**
     * The name of the Docker compose project used to generate container names.
     *
     * Maps to `--project-name` argument
     */
    val projectName: Property<String>

    /**
     * The working directory for the `docker-compose` command.
     *
     * Defaults to the project's directory.
     */
    val workingDirectory: DirectoryProperty

    /**
     * The source locations for the `docker-compose` file.
     */
    val composeFile: ConfigurableFileCollection

    /**
     * Adds a source location for the `docker-compose` file.
     */
    fun composeFile(vararg paths: Any) = composeFile.from(paths)

    /**
     * Optional options to append to the `docker compose create` command
     *
     * Defaults to `--remove-orphans` to ensure only containers defined in the docker file are kept
     */
    val optionsCreate: ListProperty<String>

}
