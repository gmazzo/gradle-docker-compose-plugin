package io.github.gmazzo.docker.compose

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class DockerContainer(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @Serializable(with = InstantSerializer::class) @SerialName("Created") val created: Instant,
    @SerialName("Path") val path: String,
    @SerialName("Args") val args: List<String>,
    @SerialName("State") val state: State,
    @SerialName("Image") val image: String,
    @SerialName("NetworkSettings") val networkSettings: NetworkSettings,
) {

    @Serializable
    public data class State(
        @SerialName("Status") val status: String,
        @SerialName("Running") val running: Boolean,
        @SerialName("Paused") val paused: Boolean,
        @SerialName("Restarting") val restarting: Boolean,
        @SerialName("OOMKilled") val oomKilled: Boolean,
        @SerialName("Dead") val dead: Boolean,
        @SerialName("Pid") val pid: Int,
        @SerialName("ExitCode") val exitCode: Int,
        @SerialName("Error") val error: String?,
        @Serializable(with = InstantSerializer::class) @SerialName("StartedAt") val startedAt: Instant?,
        @Serializable(with = InstantSerializer::class) @SerialName("FinishedAt") val finishedAt: Instant?,
    )

    @Serializable
    public data class NetworkSettings(
        @SerialName("IPAddress") val ipAddress: String,
        @SerialName("Ports") val ports: Map<String, List<Ports>?>,
    )

    @Serializable
    public data class Ports(
        @SerialName("HostIp") val hostIp: String,
        @SerialName("HostPort") val hostPort: Int,
    )

}
