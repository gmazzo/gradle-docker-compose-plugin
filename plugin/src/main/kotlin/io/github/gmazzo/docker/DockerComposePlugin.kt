package io.github.gmazzo.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension

class DockerComposePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply<DockerComposeBasePlugin>()

        val extension: DockerComposeExtension = extensions.getByType()

        fun TaskProvider<*>.bindToSourceSet(sourceSet: String) = configure task@{
            extension.services.maybeCreate(sourceSet).bindTo(this@task)
        }

        plugins.withId("application") {
            tasks.named(ApplicationPlugin.TASK_RUN_NAME).bindToSourceSet(SourceSet.MAIN_SOURCE_SET_NAME)
        }

        plugins.withId("jvm-test-suite") {
            @Suppress("UnstableApiUsage")
            the<TestingExtension>().suites.withType<JvmTestSuite> suite@{
                targets.configureEach {
                    testTask.bindToSourceSet(this@suite.name)
                }
            }
        }

        plugins.withId("org.springframework.boot") {
            tasks.named("bootRun").bindToSourceSet(SourceSet.MAIN_SOURCE_SET_NAME)
            runCatching { tasks.named("bootTestRun").bindToSourceSet(SourceSet.TEST_SOURCE_SET_NAME) }
        }
    }

}
