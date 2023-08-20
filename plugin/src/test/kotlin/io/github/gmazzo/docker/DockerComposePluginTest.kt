package io.github.gmazzo.docker

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.base.TestingExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            }
    }

    private val extension: DockerComposeExtension by lazy { project.extensions.getByType() }

    @Test
    fun `creates a service per source set`() {
        assertEquals(setOf("main", "test", "integrationTest"), extension.specs.names)
    }

    @Test
    fun `only services with compose file are bound to Test task`() {
        project.layout.projectDirectory
            .file("src/integrationTest/docker-compose.yml").asFile.apply { parentFile.mkdirs() }.createNewFile()

        val test: DefaultTask by project.tasks
        val integrationTest: DefaultTask by project.tasks

        assertFalse(test.requiredServices.isServiceRequired(extension.service("test")))
        assertTrue(integrationTest.requiredServices.isServiceRequired(extension.service("integrationTest")))
    }

}