plugins {
    base
}

val pluginBuild = gradle.includedBuild("plugin")

tasks.check {
    dependsOn(pluginBuild.task(":check"))
}

tasks.build {
    dependsOn(pluginBuild.task(":build"))
}

tasks.register("publish") {
    dependsOn(pluginBuild.task(":publish"))
}
