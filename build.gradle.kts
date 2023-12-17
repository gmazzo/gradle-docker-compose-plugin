plugins {
    alias(libs.plugins.publicationsReport)
}

val pluginBuild = gradle.includedBuild("plugin")

tasks.register(LifecycleBasePlugin.BUILD_TASK_NAME) {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.register(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.register(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
    dependsOn(pluginBuild.task(":$name"))
    finalizedBy(tasks.reportPublications)
}

tasks.register(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME) {
    dependsOn(pluginBuild.task(":$name"))
    finalizedBy(tasks.reportPublications)
}

allprojects {
    tasks.withType<JacocoReport>().configureEach {
        reports.xml.required = true
    }
}