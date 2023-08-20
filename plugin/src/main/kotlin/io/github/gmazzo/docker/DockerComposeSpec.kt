package io.github.gmazzo.docker

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

abstract class DockerComposeSpec : Named {

    abstract val command: Property<String>

    abstract val commandExtraArgs: ListProperty<String>

    abstract val composeFile: ConfigurableFileCollection

    abstract val workingDirectory: DirectoryProperty

    abstract val exclusive: Property<Boolean>

    lateinit var service: Provider<DockerComposeService>
        internal set

}
