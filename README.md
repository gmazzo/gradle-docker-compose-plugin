![GitHub](https://img.shields.io/github/license/gmazzo/gradle-docker-compose-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.gmazzo.docker.compose/io.github.gmazzo.docker.compose.gradle.plugin)](https://central.sonatype.com/artifact/io.github.gmazzo.docker.compose/io.github.gmazzo.docker.compose.gradle.plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.docker.compose)](https://plugins.gradle.org/plugin/io.github.gmazzo.docker.compose)
[![Build Status](https://github.com/gmazzo/gradle-docker-compose-plugin/actions/workflows/ci-cd.yaml/badge.svg)](https://github.com/gmazzo/gradle-docker-compose-plugin/actions/workflows/ci-cd.yaml)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-docker-compose-plugin/branch/main/graph/badge.svg?token=ExYkP1Q9oE)](https://codecov.io/gh/gmazzo/gradle-docker-compose-plugin)
[![Users](https://img.shields.io/badge/users_by-Sourcegraph-purple)](https://sourcegraph.com/search?q=content:io.github.gmazzo.docker.compose+-repo:github.com/gmazzo/gradle-docker-compose-plugin)

[![Contributors](https://contrib.rocks/image?repo=gmazzo/gradle-docker-compose-plugin)](https://github.com/gmazzo/gradle-docker-compose-plugin/graphs/contributors)

# gradle-docker-compose-plugin

Spawns Docker Compose environments for tasks as
a [Shared Build Service](https://docs.gradle.org/current/userguide/build_services.html)

## Why a `BuildService`?

Comparing with a traditional `Task` approach, `BuildService`s will integrate better with Gradle's `UP-TO-DATE` and
`FROM-CACHE` states, spawning the containers **only** if the target task is effectively run.

# Usage

Apply the plugin in your project's buildscript:

```kotlin
plugins {
    id("io.github.gmazzo.docker.compose") version "<latest>"
}
```

And then add a `docker-compose.yml` (or `.yaml` or `.json`) file under the specific source set:

- `src/main/docker-compose.yml` for the `run` when using the `application` plugin (or `bootRun` when using
  `org.springframework.boot`)
- `src/test/docker-compose.yml` for the `test` task (when applying the `java` or `jvm-test-suite` plugins)
- `src/integrationTest/docker-compose.yml` for the `integrationTest` task (created by
  following [Declare an additional test suite](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html#sec:declare_an_additional_test_suite)
  Gradle manual)

## Consuming containers from code

When the service is started, the containers defined are exposed to the underlying JVM process as system properties.

For instance, given this `docker-compose.yaml` file

```yaml
services:
  app:
    image: nginx
    ports:
      - 127.0.0.1:8080:80
  db:
    image: mysql:5.7.43
    deploy:
        replicas: 3
    ports:
      - :3306
```

You will find in the following JVM system properties and environment variables:

```
Properties of `integrationTest` Docker service:
┌────────────────────────────────────────┬────────────────────────────────────────┬───────────┐
│ JVM System Property                    │ Environment Variable                   │ Value     │
├────────────────────────────────────────┼────────────────────────────────────────┼───────────┤
│ container.integrationTest-app.host     │ CONTAINER_INTEGRATIONTEST_APP_HOST     │ 127.0.0.1 │
│ container.integrationTest-app.tcp80    │ CONTAINER_INTEGRATIONTEST_APP_TCP80    │ 8080      │
│ container.integrationTest-db.host      │ CONTAINER_INTEGRATIONTEST_DB_HOST      │ 127.0.0.1 │
│ container.integrationTest-db.tcp3306   │ CONTAINER_INTEGRATIONTEST_DB_TCP3306   │ 58218     │
│ container.integrationTest-db-2.host    │ CONTAINER_INTEGRATIONTEST_DB_2_HOST    │ 127.0.0.1 │
│ container.integrationTest-db-2.tcp3306 │ CONTAINER_INTEGRATIONTEST_DB_2_TCP3306 │ 58217     │
│ container.integrationTest-db-3.host    │ CONTAINER_INTEGRATIONTEST_DB_3_HOST    │ 127.0.0.1 │
│ container.integrationTest-db-3.tcp3306 │ CONTAINER_INTEGRATIONTEST_DB_3_TCP3306 │ 58216     │
└────────────────────────────────────────┴────────────────────────────────────────┴───────────┘
```

Structure of the JVM system property:

```
 - container.main-app-2.tcp80 -> 4409
             │    │   │ │  │     └ exposed port on the host machine
             │    │   │ ├──┼ port
             │    │   │ │  └ number of the port
             │    │   │ └ type of the port
             ├────┼───┼ container name
             │    │   └ optional: id of the replica in case of more than 1
             │    └ name of the service defined in the compose file
             └ name of the source set
```

You can consume this by using `System.getProperty`:

```kotlin
val appHost = System.getProperty("container.main-app.host")
val appPort = System.getProperty("container.main-app.tcp80")
// or
val `CONTAINER_MAIN_APP_HOST` by System.getenv()
val `CONTAINER_MAIN_APP_TCP80` by System.getenv()
```

Or in Spring, by using `@Value` annotation:

```kotlin
@Value("\${container.main-app.host}:\${container.main-app.tcp80}")
private lateinit var appEndpoint: String
```

## Log in to a Docker registry or cloud backend

The `dockerCompose.login` API allowing you to provide a `server` and optionally a `username/password` that will perform
`docker login` command before any `DockerComposeService` is started

### Logging in into Amazon ECR

The following is an example on how to pipe the password obtained by `aws ecr get-login-password` command into the
`dockerCompose.login.password` DSL:

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

## Multi module setup

This plugin can be applied on multiple submodules independently without any conflicts: each `DockerComposeService` will
be unique per project and source set.

To avoid complex setup and have to replicate the shared configuration between project (like `dockerCompose.login` DSL),
if the plugin is applied at root project, all child modules will default to the settings set there.

So, at root project you can have a shared setup like this:

```kotlin
dockerCompose {
    verbose.set(false)
    login {
        username.set("myUser")
        password.set("myPass")
    }
}
```

and other submodules will default to it as well.

## Pre-initialize containers

Per each `DockerComposeService` registered (`main`, `test`, etcs...), an **optional** initialization task will be added
to the build.

For the `main` service it will be `initContainers`, `test` will be `initTestContainers`, `integrationTest` will be
`initIntegrationTestContainers` and so on.

You may run the Gradle build targeting this specific task to "win some time" by creating (but not starting) the
containers (downloading their images in the process): `docker compose create` command.

You can pass `--start` to the `initContainers` (or `initTestContainers`, etc...) task to force them to start.

> [!NOTE]
> Keep in mind there is no way to know if the target task (`run`, `test`, etc...) will be actually run until it reaches
> the point the `UP-TO-DATE` check is done, just before its about to run. So pre-creating the containers with those init
`Task`s may be a waste of resources, but it may save some overall time if actually ends up running.
> Choose wisely.

## Decoupling from JVM plugins

By default, this plugin binds automatically with `java`, `application`, `jvm-test-suite` and `org.springframework.boot`
plugins, creating default services per registered `SourceSet` (`main`, `test`, etc...).

If you don't need this behavior and want to configure and bind the `DockerComposeService`s on your own, you should apply
`io.github.gmazzo.docker.compose.base` instead.

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
