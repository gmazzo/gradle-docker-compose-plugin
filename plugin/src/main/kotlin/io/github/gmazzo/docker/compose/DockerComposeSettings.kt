package io.github.gmazzo.docker.compose

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested

interface DockerComposeSettings : DockerComposeCreateSettings {

    /**
     * Optional options to append to the `docker compose up` command.
     *
     * For consistency, [optionsCreate] will be propagated to [optionsUp].
     *
     * Defaults to `--wait` to assure containers are [healthy](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck) before moving forward with the build
     */
    val optionsUp: ListProperty<String>

    /**
     * Optional options to append to the `docker compose down` command
     */
    val optionsDown: ListProperty<String>

    /**
     * If containers should not be automatically shutdown after the build is finished or not
     */
    val keepContainersRunning: Property<Boolean>

    /**
     * If ports mapping table should be printed to the Gradle standard output when containers are started or not
     */
    val printPortMappings: Property<Boolean>

    /**
     * If logs from the running containers should be printed to the Gradle standard output or not
     */
    val printLogs: Property<Boolean>

    /**
     * Allows configuring the waiting for TCP ports
     */
    @get:Nested
    val waitForTCPPorts: WaitForTCPPorts

    fun waitForTCPPorts(action: Action<WaitForTCPPorts>) = action.execute(waitForTCPPorts)

    interface WaitForTCPPorts {

        /**
         * Instructs the service to wait for TCP ports, enabled by default.
         */
        val enabled: Property<Boolean>

        /**
         * Ports to wait to be open and accepting connections. If missing, waits for all ports
         *
         * The format should `<containerName>:tcp<portNumber>`. i.e. `main-app-1:tcp80`
         */
        val include: SetProperty<String>

        /**
         * Ports to exclude from waiting to be open and accepting connections. If missing, does not exclude any
         *
         * The format should `<containerName>:tcp<portNumber>`. i.e. `main-app-1:tcp80`
         */
        val exclude: SetProperty<String>

        /**
         * The maximum time to wait for the ports to become available (in milliseconds). Defaults to 1 minute
         */
        val timeout: Property<Int>

    }

}
