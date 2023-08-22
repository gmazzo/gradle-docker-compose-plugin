@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    `java-integration-tests`
    `git-versioning`
}

group = "io.github.gmazzo.docker"
description = "Docker Gradle Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    compileOnly(gradleKotlinDsl())

    testImplementation(gradleKotlinDsl())
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-docker-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-docker-plugin")

    plugins.create("docker") {
        id = "io.github.gmazzo.docker"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.DockerComposePlugin"
        description = "Spawns Docker Compose environments for Test tasks as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }

    plugins.create("dockerBase") {
        id = "io.github.gmazzo.docker-base"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.DockerComposeBasePlugin"
        description = "Spawns Docker Compose environments for tasks as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }

    testSourceSets(sourceSets["integrationTest"])
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all-compatibility"
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    workingDir(provider { temporaryDir })
}

tasks.integrationTest {
    shouldRunAfter(tasks.test)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
