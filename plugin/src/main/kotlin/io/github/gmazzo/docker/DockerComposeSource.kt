package io.github.gmazzo.docker

import org.gradle.api.file.ConfigurableFileCollection

interface DockerComposeSource : DockerComposeSettings {

    /**
     * The source locations for the `docker-compose` file.
     */
    val composeFile: ConfigurableFileCollection

}
