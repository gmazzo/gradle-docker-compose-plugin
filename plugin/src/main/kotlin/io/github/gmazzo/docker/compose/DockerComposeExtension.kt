package io.github.gmazzo.docker.compose

import org.gradle.api.NamedDomainObjectContainer

interface DockerComposeExtension : DockerSettings, DockerComposeSettings {

    /**
     * The registered [DockerComposeService] services
     */
    val services: NamedDomainObjectContainer<DockerComposeSpec>

}
