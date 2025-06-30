@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.gitVersion)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    jacoco
}

group = "io.github.gmazzo.docker.compose"
description = "Spawns Docker Compose environments for main code and test suites as a Gradle's Shared Build Service"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))
kotlin.compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    compileOnly(gradleKotlinDsl())

    implementation(libs.kotlin.serialization.json)

    testImplementation(gradleKotlinDsl())
    testImplementation(libs.kotlin.test)
}

val originUrl = providers
    .exec { commandLine("git", "remote", "get-url", "origin") }
    .standardOutput.asText.map { it.trim() }

gradlePlugin {
    website = originUrl
    vcsUrl = originUrl

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
        description = project.description
        tags.addAll("docker", "docker-compose", "build-service", "shared-build-service")
    }
}

mavenPublishing {
    publishToMavenCentral("CENTRAL_PORTAL", automaticRelease = true)

    pom {
        name = "${rootProject.name}-${project.name}"
        description = provider { project.description }
        url = originUrl

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
            }
        }

        developers {
            developer {
                id = "gmazzo"
                name = id
                email = "gmazzo65@gmail.com"
            }
        }

        scm {
            connection = originUrl
            developerConnection = originUrl
            url = originUrl
        }
    }
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

afterEvaluate {
    tasks.named<Jar>("javadocJar") {
        from(tasks.dokkaGeneratePublicationJavadoc)
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(tasks.publishPlugins)
}

tasks.publishPlugins {
    enabled = "$version".matches("\\d+(\\.\\d+)+".toRegex())
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
