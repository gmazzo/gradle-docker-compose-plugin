@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    `java-integration-tests`
}

description = "Docker Gradle Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    compileOnly(gradleKotlinDsl())

    testImplementation(gradleKotlinDsl())
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-docker-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-docker-plugin")

    plugins.create("docker") {
        id = "io.github.gmazzo.docker"
        displayName = name
        implementationClass = "io.github.gmazzo.docker.DockerPlugin"
        description = "Spawns Docker Compose environments for tasks as a Gradle's Shared Build Service"
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }

    testSourceSets(sourceSets["integrationTest"])
}

tasks.integrationTest {
    shouldRunAfter(tasks.test)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
