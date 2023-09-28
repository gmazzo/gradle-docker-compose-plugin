@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.docker.compose")
    application
    alias(libs.plugins.spring.boot)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

application.mainClass = "io.github.gmazzo.docker.compose.demo.SampleAppKt"

dependencies {
    implementation(libs.mysql.connector)
    implementation(libs.spring.starter.jdbc)
    implementation(libs.spring.starter.web)
}

testing.suites {
    withType<JvmTestSuite> {
        useKotlinTest()
    }

    val integrationTest by registering(JvmTestSuite::class) {
        dependencies {
            implementation(project())
            implementation(libs.mysql.connector)
        }

        // not necessary, just to validate this task works correctly as part of the CI
        targets.all { testTask { dependsOn(tasks.named("init${name.capitalize()}Containers")) } }
    }

    tasks.check {
        dependsOn(integrationTest)
    }

}
