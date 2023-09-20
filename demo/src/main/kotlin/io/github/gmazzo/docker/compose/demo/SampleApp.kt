package io.github.gmazzo.docker.compose.demo

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.net.URL

fun main(args: Array<String>) {
    runApplication<SampleApp>(*args)
}

@SpringBootApplication
abstract class SampleApp : InitializingBean {

    @Value("\${container.main-app-1.tcp80}")
    private lateinit var appEndpoint: String

    override fun afterPropertiesSet() {
        URL("http://$appEndpoint").openStream().reader().use { it.readText().also(::println) }
    }

}
