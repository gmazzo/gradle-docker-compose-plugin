plugins {
    base
}

tasks {
    val pluginBuild = gradle.includedBuild("plugin")

    sequenceOf(
        check,
        build,
        register("publish"),
        register("publishToMavenLocal"),
    ).forEach {
        it.configure { dependsOn(pluginBuild.task(":$name")) }
    }
}
