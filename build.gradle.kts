plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    `git-versioning`
}

allprojects {

    group = "io.github.gmazzo.docker"

    plugins.withId("java") {

        apply(plugin = "jacoco-report-aggregation")

        dependencies {
            "testImplementation"(libs.kotlin.test)
            "testImplementation"("org.junit.jupiter:junit-jupiter-params")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            workingDir(provider { temporaryDir })
        }

        tasks.withType<JacocoReport>().configureEach {
            reports.xml.required.set(true)
        }

        tasks.named("check") {
            dependsOn(tasks.withType<JacocoReport>())
        }

    }

}
