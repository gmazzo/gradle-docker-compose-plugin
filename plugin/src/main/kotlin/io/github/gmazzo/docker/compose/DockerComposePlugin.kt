package io.github.gmazzo.docker.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension

class DockerComposePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply<DockerComposeBasePlugin>()

        val extension: DockerComposeExtension = extensions.getByType()

        plugins.withId("application") {
            val main = extension.services.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME)

            tasks.named(ApplicationPlugin.TASK_RUN_NAME) {
                main.bindTo(this)
            }
        }

        plugins.withId("jvm-test-suite") {
            @Suppress("UnstableApiUsage")
            the<TestingExtension>().suites.withType<JvmTestSuite> suite@{
                val test = extension.services.maybeCreate(this@suite.name)

                targets.configureEach {
                    test.bindTo(testTask)
                }
            }
        }

        plugins.withId("org.springframework.boot") {
            val main = extension.services.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME)
            val test = extension.services.maybeCreate(SourceSet.TEST_SOURCE_SET_NAME)

            tasks.named("bootRun") {
                main.bindTo(this)
            }
            runCatching {
                tasks.named("bootTestRun") {
                    test.bindTo(this)
                }
            }
        }
    }

}
