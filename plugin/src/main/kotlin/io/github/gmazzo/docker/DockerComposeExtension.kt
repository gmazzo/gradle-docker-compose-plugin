package io.github.gmazzo.docker

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.get

interface DockerComposeExtension {

    val specs: NamedDomainObjectContainer<DockerComposeSpec>

    fun service(name: String): Provider<DockerComposeService> = specs[name].service

}
