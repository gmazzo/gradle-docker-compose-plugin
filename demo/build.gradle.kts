@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.docker")
    application
    alias(libs.plugins.spring.boot)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

application.mainClass = "io.github.gmazzo.docker.demo.SampleAppKt"

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
    }

    tasks.check {
        dependsOn(integrationTest)
    }
}
