package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension

class DockerComposePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "jvm-test-suite")
        apply<DockerComposeBasePlugin>()

        val extension: DockerComposeExtension = extensions.getByType()

        @Suppress("UnstableApiUsage")
        the<TestingExtension>().suites.withType<JvmTestSuite> suite@{
            val name = this@suite.name
            val sourceSetDir = layout.projectDirectory.dir("src/$name")

            extension.services.maybeCreate(name).composeFile.from(
                sourceSetDir.file("docker-compose.yml"),
                sourceSetDir.file("docker-compose.yaml"),
                sourceSetDir.file("docker-compose.json"),
            )

            targets.configureEach {
                extension.services.maybeCreate(name).bindTo(testTask)
            }
        }
    }

}
