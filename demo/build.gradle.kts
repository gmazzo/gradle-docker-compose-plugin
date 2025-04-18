@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.docker.compose")
    application
    alias(libs.plugins.spring.boot)
    jacoco
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

application.mainClass = "io.github.gmazzo.docker.compose.demo.SampleAppKt"

testing.suites.withType<JvmTestSuite> {
    useKotlinTest()
}

testing.suites.register<JvmTestSuite>("integrationTest") {
    // not necessary, just to validate this task works correctly as part of the CI
    targets.all { testTask { dependsOn(tasks.named("init${name.replaceFirstChar { it.uppercase() }}Containers")) } }
}

tasks.check {
    dependsOn(testing.suites.withType<JvmTestSuite>())
}

dependencies {
    implementation(libs.mysql.connector)
    implementation(libs.spring.starter.jdbc)
    implementation(libs.spring.starter.web)
    testImplementation(libs.spring.starter.test)
    "integrationTestImplementation"(libs.mysql.connector)
}

// tests just runs the main application, so we tell it to use the main docker compose
dockerCompose.services.named("main") {
    bindTo(tasks.test)
}
