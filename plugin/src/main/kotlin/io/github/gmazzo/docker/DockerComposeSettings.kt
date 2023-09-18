package io.github.gmazzo.docker

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface DockerComposeSettings {

    val projectName: Property<String>

    val command: Property<String>

    val commandExtraArgs: ListProperty<String>

    val composeFile: ConfigurableFileCollection

    val workingDirectory: DirectoryProperty

    val printLogs: Property<Boolean>

}
