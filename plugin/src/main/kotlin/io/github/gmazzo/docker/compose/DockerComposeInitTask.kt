package io.github.gmazzo.docker.compose

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class DockerComposeInitTask : DefaultTask(), DockerComposeCreateSettings {

    @get:Internal
    abstract val dockerService: Property<DockerService>

    @get:Internal
    abstract val dockerComposeService: Property<DockerComposeService>

    @get:Input
    abstract override val projectName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract override val composeFile: ConfigurableFileCollection

    @get:Internal
    abstract override val workingDirectory: DirectoryProperty

    @get:Input
    abstract override val optionsCreate: ListProperty<String>

    @get:Input
    @Suppress("unused")
    internal val workingDirectoryPath
        get() = workingDirectory.map { it.asFile.toRelativeString(projectDir) }

    @get:Input
    @get:Optional
    @get:Option(option = "start", description = "Starts the containers, instead of just creating then")
    abstract val start: Property<Boolean>

    private val projectDir = project.projectDir

    @TaskAction
    fun initContainers() {
        if (start.getOrElse(false)) {
            dockerComposeService.get()

        } else {
            dockerService.get().composeExec(this, "create", *optionsCreate.get().toTypedArray())
        }
    }

}
