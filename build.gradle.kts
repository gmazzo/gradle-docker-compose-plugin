plugins {
    base
    `maven-publish`
    alias(libs.plugins.publicationsReport)
}

val pluginBuild = gradle.includedBuild("plugin")

tasks.build {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.check {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.publish {
    dependsOn(pluginBuild.task(":$name"))
    finalizedBy(tasks.reportPublications)
}

tasks.publishToMavenLocal {
    dependsOn(pluginBuild.task(":$name"))
    finalizedBy(tasks.reportPublications)
}

allprojects {
    tasks.withType<JacocoReport>().configureEach {
        reports.xml.required = true
    }
}