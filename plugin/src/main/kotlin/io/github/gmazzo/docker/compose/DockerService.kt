package io.github.gmazzo.docker.compose

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

@Suppress("LeakingThis")
abstract class DockerService @Inject constructor(
    private val execOperations: ExecOperations,
) : BuildService<DockerService.Params>, Runnable {

    private val logger = LoggerFactory.getLogger(DockerService::class.java)

    init {
        run()
    }

    override fun run() {
        parameters.login.server.orNull?.let { server ->
            logger.info("Performing Docker login to `$server`...")

            val user = parameters.login.username.orNull
            val password = parameters.login.password.orNull

            exec("login", server,
                *user?.let { arrayOf("--username", it, "--password-stdin") }.orEmpty()){
                if (password != null) standardInput = password.byteInputStream()
            }
        }
    }

    @JvmOverloads
    fun exec(
        vararg command: String,
        action: Action<in ExecSpec>? = null,
    ): ExecResult = execOperations.exec {
        executable = parameters.command.get()
        args = parameters.options.get() + command
        action?.execute(this)

        if (parameters.verbose.get()) {
            logger.info("dockerExec: `{}`", commandLine.joinToString(separator = " "))
        }
    }

    @JvmOverloads
    fun composeExec(
        source: DockerComposeSource,
        vararg command: String,
        action: Action<in ExecSpec>? = null,
    ) = source.workingDirectory.get().asFile.let { workingDirectory ->
        exec(
            "compose",
            "--project-name",
            source.projectName.get(),
            "-f",
            source.composeFile.singleFileOrThrow.toRelativeString(workingDirectory),
            *command,
        ) {
            workingDir = workingDirectory
            action?.execute(this)
        }
    }

    private val FileCollection.singleFileOrThrow: File
        get() = with(asFileTree) {
            check(!isEmpty) {
                this@singleFileOrThrow.joinToString(
                    prefix = "No `docker-compose` files found at:",
                    separator = ""
                ) { "\n - $it" }
            }
            try {
                return singleFile

            } catch (e: IllegalStateException) {
                error(files.joinToString(e.message.orEmpty()) { "\n - $it" })
            }
        }

    interface Params : BuildServiceParameters, DockerSettings

}
