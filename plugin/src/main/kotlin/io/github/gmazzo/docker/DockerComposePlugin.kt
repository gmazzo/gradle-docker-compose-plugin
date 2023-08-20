package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension

class DockerComposePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply<DockerComposeBasePlugin>()
        apply(plugin = "jvm-test-suite")

        val extension: DockerComposeExtension = extensions.getByType()

        the<SourceSetContainer>().configureEach ss@{
            val sourceSetDir = layout.projectDirectory.dir("src/${this@ss.name}")

            extension.specs.maybeCreate(this@ss.name).composeFile.from(
                sourceSetDir.file("docker-compose.yml"),
                sourceSetDir.file("docker-compose.yaml"),
                sourceSetDir.file("docker-compose.json"),
            )
        }

        @Suppress("UnstableApiUsage")
        the<TestingExtension>().suites.withType<JvmTestSuite>().configureEach suite@{
            targets.configureEach {
                testTask.configure {
                    if (!extension.specs[this@suite.name].composeFile.asFileTree.isEmpty) {
                        usesService(extension.service(this@suite.name))
                    }
                }
            }
        }
    }

}
