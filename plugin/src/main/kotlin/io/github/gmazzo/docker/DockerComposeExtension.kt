package io.github.gmazzo.docker

import org.gradle.api.NamedDomainObjectContainer

interface DockerComposeExtension : DockerComposeSettings {

    val services: NamedDomainObjectContainer<DockerComposeSpec>

}
