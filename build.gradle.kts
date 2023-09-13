plugins {
    base
}

val pluginBuild = gradle.includedBuild("plugin")

tasks.check {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.build {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.register("publish") {
    dependsOn(pluginBuild.task(":$name"))
}

tasks.register("publishToMavenLocal") {
    dependsOn(pluginBuild.task(":$name"))
}
