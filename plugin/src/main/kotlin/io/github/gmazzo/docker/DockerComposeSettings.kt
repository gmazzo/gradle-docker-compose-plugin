package io.github.gmazzo.docker

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

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
     * If logs from the running containers should be printed to the Gradle standard output or not
     */
    val verbose: Property<Boolean>

}
