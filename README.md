![GitHub](https://img.shields.io/github/license/gmazzo/gradle-docker-compose-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.docker.compose)](https://plugins.gradle.org/plugin/io.github.gmazzo.docker.compose)
![Build Status](https://github.com/gmazzo/gradle-docker-compose-plugin/actions/workflows/build.yaml/badge.svg)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-docker-compose-plugin/branch/main/graph/badge.svg?token=ExYkP1Q9oE)](https://codecov.io/gh/gmazzo/gradle-docker-compose-plugin)

# gradle-docker-compose-plugin
Spawns Docker Compose environments for tasks as a [Shared Build Service](https://docs.gradle.org/current/userguide/build_services.html)

## Why a `BuildService`?
Comparing with a traditional `Task` approach, `BuildService`s will integrate better with Gradle's `UP-TO-DATE` and `FROM-CACHE` states, spawning the containers **only** if the target task is effectively run.

# Usage
Apply the plugin in your project's buildscript:
```kotlin
plugins {
    id("io.github.gmazzo.docker.compose") version "<latest>"
}
```
And then add a `docker-compose.yml` (or `.yaml` or `.json`) file under the specific source set:

- `src/main/docker-compose.yml` for the `run` when using the `application` plugin (or `bootRun` when using `org.springframework.boot`)
- `src/test/docker-compose.yml` for the `test` task (when applying the `java` or `jvm-test-suite` plugins)
- `src/test/docker-compose.yml` for the `integrationTest` task (created by following [Declare an additional test suite](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html#sec:declare_an_additional_test_suite) Gradle manual)

## Consuming containers from code
When the service is started, the containers defined are exposed to the underlying JVM process as system properties.

For instance, given this `docker-compose.yaml` file
```yaml
services:
  app:
    image: yeasy/simple-web
    ports:
      - 8080:80
```
You will find in the following property:
```
 - container.main-app-1.tcp80 -> 0.0.0.0:8080
             │    │   │ │  │     └ exposed host and port on the host machine
             │    │   │ ├──┼ port
             │    │   │ │  └ number of the port
             │    │   │ └ type of the port
             ├────┼───┼ container name
             │    │   └ id of the replica
             │    └ name of the service defined in the compose file
             └ name of the source set
```
You can consume this by using `System.getProperty`:
```kotlin
val appEndpoint = System.getProperty("container.main-app-1.tcp80")
```
Or in Spring, by using `@Value` annotation:
```kotlin
@Value("\${container.main-app-1.tcp80}")
private lateinit var appEndpoint: String
```

## Log in to a Docker registry or cloud backend
The `dockerCompose.login` API allowing you to provide a `server` and optionally a `username/password` that will perform `docker login` command before any `DockerComposeService` is started

### Logging in into Amazon ECR
The following is an example on how to pipe the password obtained by `aws ecr get-login-password` command into the `dockerCompose.login.password` DSL:
```kotlin
dockerCompose {
    login {
        val awsPassword = providers
            .exec { commandLine("aws", "ecr", "get-login-password", "--region", "eu-west-1") }
            .standardOutput.asText
        
        server.set("<accountId>.dkr.ecr.eu-west-1.amazonaws.com")
        username.set("AWS")
        password.set(awsPassword)
    }
}
```

## Pre-initialize containers
Per each `DockerComposeService` registered (`main`, `test`, etcs...), an **optional** initialization task will be added to the build.

For the `main` service it will be `initContainers`, `test` will be `initTestContainers`, `integrationTest` will be `initIntegrationTestContainers` and so on.

You may run the Gradle build targeting this specific task to "win some time" by creating (but not starting) the containers (downloading their images in the process): `docker compose create` command.

> [!NOTE]
> Keep in mind there is no way to know if the target task (`run`, `test`, etc...) will be actually run until it reaches the point the `UP-TO-DATE` check is done, just before its about to run. So pre-creating the containers with those init `Task`s may be a waste of resources, but it may save some overall time if actually ends up running. 
> Choose wisely.

## Decoupling from JVM plugins
By default, this plugin binds automatically with `java`, `application`, `jvm-test-suite` and `org.springframework.boot` plugins, creating default services per registered `SourceSet` (`main`, `test`, etc...).

If you don't need this behavior and want to configure and bind the `DockerComposeService`s on your own, you should apply `io.github.gmazzo.docker.compose.base` instead.

Later use the `DockerComposeSpec.bindTo(Task)` API 

An example could be:
```kotlin
plugins {
    id("io.github.gmazzo.docker.compose.base")
}

val myService = dockerCompose.services.create("myService") {
    composeFile.from(file("my-docker-compose.yaml"))
}

tasks.register("myTask") {
    myService.bindTo(this)
}
```
