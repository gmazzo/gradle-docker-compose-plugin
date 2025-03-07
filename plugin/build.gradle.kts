@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    signing
    jacoco
}

group = "io.github.gmazzo.docker.compose"
description = "Docker Compose Gradle Plugin"
version = providers
    .exec { commandLine("git", "describe", "--tags", "--always") }
    .standardOutput.asText.get().trim().removePrefix("v")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))
kotlin.compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    compileOnly(gradleKotlinDsl())

    implementation(libs.kotlin.serialization.json)

    testImplementation(gradleKotlinDsl())
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-docker-compose-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-docker-compose-plugin")

    plugins.create("dockerComposeBase") {
        id = "io.github.gmazzo.docker.compose.base"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.compose.DockerComposeBasePlugin"
        description = "Spawns Docker Compose environments for tasks as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }

    plugins.create("dockerCompose") {
        id = "io.github.gmazzo.docker.compose"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.compose.DockerComposePlugin"
        description = "Spawns Docker Compose environments for main code and test suites as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)
    publishing.publications.configureEach(::sign)
    tasks.withType<Sign>().configureEach { enabled = signingKey != null }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    workingDir(provider { temporaryDir })
}

tasks.withType<JacocoReport>().configureEach {
    reports.xml.required = true
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
