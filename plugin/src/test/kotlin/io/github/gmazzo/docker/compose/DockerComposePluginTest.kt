package io.github.gmazzo.docker.compose

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.base.TestingExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DockerComposePluginTest {

    private val project by lazy {
        ProjectBuilder.builder()
            .withName("root")
            .build()
            .also {
                it.apply(plugin = "java")
                it.apply<DockerComposePlugin>()

                @Suppress("UnstableApiUsage")
                it.the<TestingExtension>().suites.register<JvmTestSuite>("integrationTest")

                it.layout.projectDirectory.file("src/integrationTest/docker-compose.yml").asFile
                    .apply { parentFile.mkdirs() }
                    .writeText("""
                        services:
                            app:
                                image: nginx
                                ports:
                                  - 127.0.0.1:8080:80
                            other:
                                image: nginx
                                ports:
                                  - 127.0.0.1:8090:80
                                  - 127.0.0.1:8091:81
                    """.trimIndent())
            }
    }

    private val extension: DockerComposeExtension by lazy { project.extensions.getByType() }

    @Test
    fun `creates a service per test suite`() {
        assertEquals(setOf("test", "integrationTest"), extension.services.names)
    }

    @Test
    fun `services are bound to Test task`() {
        val test: DefaultTask by project.tasks
        val integrationTest: DefaultTask by project.tasks

        assertTrue(test.requiredServices.isServiceRequired(extension.services["test"].buildService))
        assertTrue(integrationTest.requiredServices.isServiceRequired(extension.services["integrationTest"].buildService))
    }

    @Test
    fun `containers properties are propagated`() {
        val services = project.the<DockerComposeExtension>().services
        val test: DockerComposeSpec by services
        val integrationTest: DockerComposeSpec by services

        try {
            assertEquals(emptyMap(), test.buildService.get().containersAsSystemProperties)
            assertEquals(
                mapOf(
                    "container.integrationTest-app.host" to "127.0.0.1",
                    "container.integrationTest-app.tcp80" to "8080",
                    "container.integrationTest-other.host" to "127.0.0.1",
                    "container.integrationTest-other.tcp80" to "8090",
                    "container.integrationTest-other.tcp81" to "8091",
                ), integrationTest.buildService.get().containersAsSystemProperties
            )
            assertEquals(
                mapOf(
                    "CONTAINER_INTEGRATIONTEST_APP_HOST" to "127.0.0.1",
                    "CONTAINER_INTEGRATIONTEST_APP_TCP80" to "8080",
                    "CONTAINER_INTEGRATIONTEST_OTHER_HOST" to "127.0.0.1",
                    "CONTAINER_INTEGRATIONTEST_OTHER_TCP80" to "8090",
                    "CONTAINER_INTEGRATIONTEST_OTHER_TCP81" to "8091",
                ), integrationTest.buildService.get().containersAsEnvironmentVariables
            )
        } finally {
            services.configureEach {
                runCatching { buildService.get().close() }
            }
        }
    }

}
