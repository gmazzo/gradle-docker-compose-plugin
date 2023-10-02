package io.github.gmazzo.docker.compose

import javax.inject.Inject

internal abstract class DockerComposeChildExtension @Inject constructor(
    dockerSettings: DockerSettings,
) : DockerSettings by dockerSettings, DockerComposeExtension
