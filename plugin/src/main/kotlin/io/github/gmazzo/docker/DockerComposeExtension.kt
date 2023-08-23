package io.github.gmazzo.docker

import org.gradle.api.NamedDomainObjectContainer

interface DockerComposeExtension {

    val services: NamedDomainObjectContainer<DockerComposeSpec>

}
