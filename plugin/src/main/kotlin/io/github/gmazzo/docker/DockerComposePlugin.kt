package io.github.gmazzo.docker

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
            tasks.named(ApplicationPlugin.TASK_RUN_NAME) {
                extension.services.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME).bindTo(this)
            }
        }

        plugins.withId("jvm-test-suite") {
            @Suppress("UnstableApiUsage")
            the<TestingExtension>().suites.withType<JvmTestSuite> suite@{
                val service = extension.services.maybeCreate(this@suite.name)

                targets.configureEach {
                    service.bindTo(testTask)
                }
            }
        }

        plugins.withId("org.springframework.boot") {
            tasks.named("bootRun") {
                extension.services.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME).bindTo(this)
            }
            runCatching {
                tasks.named("bootTestRun") {
                    extension.services.maybeCreate(SourceSet.TEST_SOURCE_SET_NAME).bindTo(this)
                }
            }
        }
    }

}
