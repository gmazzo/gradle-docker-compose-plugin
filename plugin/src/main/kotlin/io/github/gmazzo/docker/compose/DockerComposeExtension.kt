package io.github.gmazzo.docker.compose

import org.gradle.api.NamedDomainObjectContainer

public interface DockerComposeExtension : DockerSettings, DockerComposeSettings {

    /**
     * The registered [DockerComposeService] services
     */
    public val services: NamedDomainObjectContainer<DockerComposeSpec>

}
