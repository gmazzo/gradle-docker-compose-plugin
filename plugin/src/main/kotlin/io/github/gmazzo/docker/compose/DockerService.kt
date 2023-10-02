package io.github.gmazzo.docker.compose

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@Suppress("LeakingThis")
abstract class DockerService @Inject constructor(
    private val execOperations: ExecOperations,
) : BuildService<DockerService.Params>, Runnable {

    private val logger = Logging.getLogger(DockerService::class.java)

    init {
        run()
    }

    override fun run() {
        parameters.login.server.orNull?.let { server ->
            logger.lifecycle("Performing Docker login to `{}`...", server)

            val user = parameters.login.username.orNull
            val password = parameters.login.password.orNull

            exec(
                "login", server,
                *user?.let { arrayOf("--username", it, "--password-stdin") }.orEmpty(),
                input = password?.byteInputStream()
            )
        }
    }

    @JvmOverloads
    fun exec(
        vararg command: String,
        workingDirectory: File? = null,
        input: InputStream? = null,
        failNonZeroExitValue: Boolean = true,
    ): ExecResult {
        lateinit var commands: List<String>
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        val both = ByteArrayOutputStream()

        val result = execOperations.exec {
            executable = parameters.command.get()
            args = parameters.options.get() + command
            if (input != null) standardInput = input
            if (workingDirectory != null) workingDir = workingDirectory
            standardOutput = TeeOutputStream(output, both)
            errorOutput = TeeOutputStream(error, both)
            isIgnoreExitValue = true
            commands = commandLine
        }
        return ExecResult(
            command = commands,
            exitValue = result.exitValue,
            standardOutput = output,
            standardError = error,
            combinedOutput = both,
        ).also {
            if (failNonZeroExitValue) {
                it.assertNormalExitValue()
            }
        }
    }

    fun composeExec(
        settings: DockerComposeCreateSettings,
        vararg command: String,
        input: InputStream? = null,
        failNonZeroExitValue: Boolean = true,
    ) = settings.workingDirectory.get().asFile.let { workingDir ->
        exec(
            "compose",
            "--project-name",
            settings.projectName.get(),
            "-f",
            settings.composeFile.singleFileOrThrow.toRelativeString(workingDir),
            *command,
            workingDirectory = workingDir,
            input = input,
            failNonZeroExitValue = failNonZeroExitValue,
        )
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

    class ExecResult(
        val command: List<String>,
        val exitValue: Int,
        standardOutput: ByteArrayOutputStream,
        standardError: ByteArrayOutputStream,
        combinedOutput: ByteArrayOutputStream,
    ) {

        val standardOutput by lazy { standardOutput.content }

        val standardError by lazy { standardError.content }

        val combinedOutput by lazy { combinedOutput.content }

        private val ByteArrayOutputStream.content
            get() = toString(StandardCharsets.UTF_8).trim()

        fun assertNormalExitValue() = check(exitValue == 0) {
            command.joinToString(
                prefix = "Command `",
                separator = " ",
                postfix = "` finished with non-zero exit value $exitValue:\n$combinedOutput"
            )
        }

    }

}
