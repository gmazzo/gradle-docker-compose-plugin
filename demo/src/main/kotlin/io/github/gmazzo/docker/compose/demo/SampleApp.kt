package io.github.gmazzo.docker.compose.demo

import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import javax.sql.DataSource

fun main(args: Array<String>) {
    runApplication<SampleApp>(*args)
}

@SpringBootApplication
abstract class SampleApp(
    private val dataSource: DataSource
) : InitializingBean {

    override fun afterPropertiesSet() {
        dataSource.connection.use {
            println("""
                
                *********************************************
                Database is ${it.metaData.databaseProductName} ${it.metaData.databaseProductVersion}
                *********************************************
                
            """.trimIndent())
        }
    }

}
