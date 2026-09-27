package io.github.gmazzo.docker.compose

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations

public abstract class DockerService @Inject constructor(
    private val execOperations: ExecOperations,
) : BuildService<BuildServiceParameters.None>,
    DockerComposeExtension,
    AutoCloseable {

    private val logger = Logging.getLogger(DockerService::class.java)

    init {
        // DockerSettings defaults
        command.convention("docker").finalizeValueOnRead()
        options.finalizeValueOnRead()
        login.server.finalizeValueOnRead()
        login.username.finalizeValueOnRead()
        login.password.finalizeValueOnRead()

        // DockerComposeSettings shared defaults
        optionsCreate.apply { add("--remove-orphans") }.finalizeValueOnRead()
        optionsUp.apply { add("--wait") }.finalizeValueOnRead()
        optionsDown.finalizeValueOnRead()
        keepContainersRunning.convention(false).finalizeValueOnRead()
        waitForTCPPorts.enabled.convention(true).finalizeValueOnRead()
        waitForTCPPorts.timeout.convention(TimeUnit.MINUTES.toMillis(1).toInt()).finalizeValueOnRead()
        printPortMappings.convention(true).finalizeValueOnRead()
        printLogs.convention(true).finalizeValueOnRead()
    }

    public var started: Boolean = false
        private set

    public fun start() {
        if (started) return
        started = true

        login.server.orNull?.let { server ->
            logger.lifecycle("Performing Docker login to `{}`...", server)

            val user = login.username.orNull
            val password = login.password.orNull

            exec(
                "login", server,
                *user?.let { arrayOf("--username", it, "--password-stdin") }.orEmpty(),
                input = password?.byteInputStream()
            )
        }
    }

    override fun close() {
        started = false
    }

    @JvmOverloads
    public fun exec(
        vararg command: String,
        workingDirectory: File? = null,
        input: InputStream? = null,
        failNonZeroExitValue: Boolean = true,
    ): ExecResult {
        lateinit var commands: List<String>
        val output = ByteArrayOutputStream()
        val outputAndError = ByteArrayOutputStream()

        val result = execOperations.exec {
            executable = this@DockerService.command.get()
            args = this@DockerService.options.get() + command
            if (input != null) standardInput = input
            if (workingDirectory != null) workingDir = workingDirectory
            standardOutput = TeeOutputStream(output, outputAndError)
            errorOutput = outputAndError
            isIgnoreExitValue = true
            commands = commandLine
        }
        return ExecResult(
            command = commands,
            exitValue = result.exitValue,
            output = output,
            outputAndError = outputAndError,
        ).also {
            if (failNonZeroExitValue) {
                it.assertNormalExitValue()
            }
        }
    }

    public fun composeExec(
        settings: DockerComposeCreateSettings,
        vararg command: String,
        input: InputStream? = null,
        failNonZeroExitValue: Boolean = true,
    ): DockerService.ExecResult = settings.workingDirectory.get().asFile.let { workingDir ->
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

    public class ExecResult(
        public val command: List<String>,
        public val exitValue: Int,
        output: ByteArrayOutputStream,
        outputAndError: ByteArrayOutputStream,
    ) {

        public val output: String by lazy { output.toString(StandardCharsets.UTF_8).trim() }

        public val outputAndError: String by lazy { outputAndError.toString(StandardCharsets.UTF_8).trim() }

        public fun assertNormalExitValue(): Unit = check(exitValue == 0) {
            command.joinToString(
                prefix = "Command `",
                separator = " ",
                postfix = "` finished with non-zero exit value $exitValue:\n$outputAndError"
            )
        }

    }

}
