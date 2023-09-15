package io.github.gmazzo.docker.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
abstract class SampleApp

fun main(args: Array<String>) {
    runApplication<SampleApp>(*args)
}
