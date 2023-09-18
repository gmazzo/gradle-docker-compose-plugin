package io.github.gmazzo.docker

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
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class DockerComposeInitTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask(), DockerComposeSource {

    @get:Input
    abstract override val projectName: Property<String>

    @get:Input
    abstract override val command: Property<String>

    @get:Input
    abstract override val commandExtraArgs: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract override val composeFile: ConfigurableFileCollection

    @get:Internal
    abstract override val workingDirectory: DirectoryProperty

    private val projectDir = project.projectDir

    @get:Input
    @Suppress("unused")
    internal val workingDirectoryPath
        get() = workingDirectory.map { it.asFile.toRelativeString(projectDir) }

    @get:Internal
    abstract override val verbose: Property<Boolean>

    @TaskAction
    fun initContainers() {
        execOperations.dockerCompose(this, "create", "--remove-orphans")
    }

}
