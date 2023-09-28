package io.github.gmazzo.docker.compose

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DockerContainer(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Created") val created: Instant,
    @SerialName("Path") val path: String,
    @SerialName("Args") val args: List<String>,
    @SerialName("State") val state: State,
    @SerialName("Image") val image: String,
    @SerialName("NetworkSettings") val networkSettings: NetworkSettings,
) {

    @Serializable
    data class State(
        @SerialName("Status") val status: String,
        @SerialName("Running") val running: Boolean,
        @SerialName("Paused") val paused: Boolean,
        @SerialName("Restarting") val restarting: Boolean,
        @SerialName("OOMKilled") val oomKilled: Boolean,
        @SerialName("Dead") val dead: Boolean,
        @SerialName("Pid") val pid: Int,
        @SerialName("ExitCode") val exitCode: Int,
        @SerialName("Error") val error: String?,
        @SerialName("StartedAt") val startedAt: Instant?,
        @SerialName("FinishedAt") val finishedAt: Instant?,
    )

    @Serializable
    data class NetworkSettings(
        @SerialName("IPAddress") val ipAddress: String,
        @SerialName("Ports") val ports: Map<String, List<Ports>?>,
    )

    @Serializable
    data class Ports(
        @SerialName("HostIp") val hostIp: String,
        @SerialName("HostPort") val hostPort: Int,
    )

}
