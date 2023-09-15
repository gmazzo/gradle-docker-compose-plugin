plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.docker")
    application
    alias(libs.plugins.spring.boot)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

dependencies {
    implementation(libs.spring.starter.web)
}

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
