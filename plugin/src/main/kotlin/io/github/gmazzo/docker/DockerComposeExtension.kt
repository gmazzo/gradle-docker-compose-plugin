package io.github.gmazzo.docker

import org.gradle.api.NamedDomainObjectContainer

interface DockerComposeExtension : DockerSettings, DockerComposeSettings {

    /**
     * The registered [DockerComposeService] services
     */
    val services: NamedDomainObjectContainer<DockerComposeSpec>

}
