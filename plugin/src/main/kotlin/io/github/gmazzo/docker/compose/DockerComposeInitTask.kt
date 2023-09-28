package io.github.gmazzo.docker.compose

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class DockerComposeInitTask : DefaultTask(), DockerComposeSource {

    @get:Internal
    abstract val dockerService: Property<DockerService>

    @get:Input
    abstract override val projectName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract override val composeFile: ConfigurableFileCollection

    @get:Internal
    abstract override val workingDirectory: DirectoryProperty

    @get:Input
    abstract override val optionsCreate: ListProperty<String>

    @get:Internal
    abstract override val optionsUp: ListProperty<String>

    @get:Internal
    abstract override val optionsDown: ListProperty<String>

    private val projectDir = project.projectDir

    @get:Input
    @Suppress("unused")
    internal val workingDirectoryPath
        get() = workingDirectory.map { it.asFile.toRelativeString(projectDir) }

    @get:Internal
    abstract override val printPortMappings: Property<Boolean>

    @get:Internal
    abstract override val printLogs: Property<Boolean>

    @TaskAction
    fun initContainers() {
        dockerService.get().composeExec(this, "create", *optionsCreate.get().toTypedArray())
    }

}
