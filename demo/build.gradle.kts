@file:Suppress("UnstableApiUsage")

import org.gradle.configurationcache.extensions.capitalized


plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.docker.compose")
    application
    alias(libs.plugins.spring.boot)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

application.mainClass = "io.github.gmazzo.docker.compose.demo.SampleAppKt"

dependencies {
    implementation(libs.spring.starter.web)
}

testing.suites {
    withType<JvmTestSuite> {
        useKotlinTest()
    }

    val integrationTest by registering(JvmTestSuite::class) {
        dependencies {
            implementation(project())
        }

        // not necessary, just to validate this task works correctly as part of the CI
        targets.all { testTask { dependsOn(tasks.named("init${name.capitalized()}Containers")) } }
    }

    tasks.check {
        dependsOn(integrationTest)
    }

}
