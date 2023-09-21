package io.github.gmazzo.docker.compose

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

@JvmDefaultWithoutCompatibility
interface DockerComposeSettings {

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
     * Optional options to append to the `docker compose create` command
     */
    val optionsCreate: ListProperty<String>

    /**
     * Optional options to append to the `docker compose up` command.
     *
     * For consistency, [optionsCreate] will be propagated to [optionsUp]
     */
    val optionsUp: ListProperty<String>

    /**
     * Optional options to append to the `docker compose down` command
     */
    val optionsDown: ListProperty<String>

    /**
     * If logs from the running containers should be printed to the Gradle standard output or not
     */
    val showLogs: Property<Boolean>

}
