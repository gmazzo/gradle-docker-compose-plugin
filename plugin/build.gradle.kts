@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.pluginPublish)
}

group = "io.github.gmazzo.docker"
description = "Docker Gradle Plugin"
version = providers
    .exec { commandLine("git", "describe", "--tags", "--always") }
    .standardOutput.asText.map { it.trim().removePrefix("v") }

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    compileOnly(gradleKotlinDsl())

    implementation(libs.kotlin.serialization.json)

    testImplementation(gradleKotlinDsl())
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-docker-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-docker-plugin")

    plugins.create("docker") {
        id = "io.github.gmazzo.docker.base"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.DockerComposeBasePlugin"
        description = "Spawns Docker Compose environments for tasks as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }

    plugins.create("dockerJVMTests") {
        id = "io.github.gmazzo.docker"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.DockerComposeJVMTestsPlugin"
        description = "Spawns Docker Compose environments for Test suites as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    workingDir(provider { temporaryDir })
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
