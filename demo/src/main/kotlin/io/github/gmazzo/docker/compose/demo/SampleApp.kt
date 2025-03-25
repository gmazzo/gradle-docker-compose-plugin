package io.github.gmazzo.docker.compose.demo

import javax.sql.DataSource
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<SampleApp>(*args)
}

@SpringBootApplication
open class SampleApp(
    private val dataSource: DataSource
) : InitializingBean {

    override fun afterPropertiesSet() {
        dataSource.connection.use {
            println(
                """

                *********************************************
                Database is ${it.metaData.databaseProductName} ${it.metaData.databaseProductVersion}
                *********************************************

                """.trimIndent()
            )
        }
    }

}
