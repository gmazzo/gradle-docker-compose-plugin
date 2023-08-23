package io.github.gmazzo.docker.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DockerContainerInfo(
    @SerialName("Command") val command: String,
    @SerialName("Created") val created: Long,
    @SerialName("ExitCode") val exitCode: Int,
    @SerialName("Health") val health: String,
    @SerialName("ID") val id: String,
    @SerialName("Image") val image: String,
    @SerialName("Name") val name: String,
    @SerialName("Project") val project: String,
    @SerialName("Publishers") val publishers: List<Publisher>,
    @SerialName("Service") val service: String,
    @SerialName("State") val state: String,
    @SerialName("Status") val status: String,
) {

    @Serializable
    data class Publisher(
        @SerialName("Protocol") val protocol: String,
        @SerialName("PublishedPort") val publishedPort: Int,
        @SerialName("TargetPort") val targetPort: Int,
        @SerialName("URL") val url: String,
    )

}