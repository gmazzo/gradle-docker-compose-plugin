![GitHub](https://img.shields.io/github/license/gmazzo/gradle-docker-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.docker)](https://plugins.gradle.org/plugin/io.github.gmazzo.docker)
![Build Status](https://github.com/gmazzo/gradle-docker-plugin/actions/workflows/build.yaml/badge.svg)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-docker-plugin/branch/main/graph/badge.svg?token=ExYkP1Q9oE)](https://codecov.io/gh/gmazzo/gradle-docker-plugin)

# gradle-docker-plugin
Spawns Docker Compose environments for tasks as a [Shared Build Service](https://docs.gradle.org/current/userguide/build_services.html)

# Usage
Apply the plugin in your project's buildscript:
```kotlin
plugins {
    id("io.github.gmazzo.docker") version "<latest>"
}
```
