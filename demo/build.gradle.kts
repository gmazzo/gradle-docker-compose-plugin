plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.docker.jvm-tests")
    `jvm-test-suite`
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))

testing.suites {
    withType<JvmTestSuite> {
        useKotlinTest()
    }

    val integrationTest by registering(JvmTestSuite::class) {
        dependencies {
            implementation(project())
        }
    }

    tasks.check {
        dependsOn(integrationTest)
    }
}
