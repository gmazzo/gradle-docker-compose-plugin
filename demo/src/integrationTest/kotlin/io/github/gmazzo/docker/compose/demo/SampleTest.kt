package io.github.gmazzo.docker.compose.demo

import java.net.URL
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleTest {

    @Test
    fun `container properties are exposed`() {
        val props = System.getProperties().filterKeys { (it as? String)?.startsWith("container.") == true }
        val envVars = System.getenv().filterKeys { it.startsWith("CONTAINER_") }

        assertEquals(
            setOf(
                "container.integrationTest-proxy.host",
                "container.integrationTest-proxy.tcp80",
                "container.integrationTest-db.host",
                "container.integrationTest-db.tcp3306",
            ), props.keys
        )
        assertEquals(
            setOf(
                "CONTAINER_INTEGRATIONTEST_PROXY_HOST",
                "CONTAINER_INTEGRATIONTEST_PROXY_TCP80",
                "CONTAINER_INTEGRATIONTEST_DB_HOST",
                "CONTAINER_INTEGRATIONTEST_DB_TCP3306",
            ), envVars.keys
        )
    }

    @Test
    fun `can connect to db`() {
        val dbHost = System.getProperty("container.integrationTest-db.host")
        val dbPort = System.getProperty("container.integrationTest-db.tcp3306")

        DriverManager.registerDriver(com.mysql.cj.jdbc.Driver())
        DriverManager.getConnection("jdbc:mysql://$dbHost:$dbPort/", "root", "test").use {
            assertEquals("MySQL", it.metaData.databaseProductName)
            assertEquals("5.7.43", it.metaData.databaseProductVersion)
        }
    }

    @Test
    fun `can fetch content from a proxy`() {
        val proxyHost = System.getProperty("container.integrationTest-proxy.host")
        val proxyPort = System.getProperty("container.integrationTest-proxy.tcp80")

        URL("http://$proxyHost:$proxyPort").openStream().use {
            assertTrue(it.bufferedReader().readText().startsWith("<!DOCTYPE html>"))
        }
    }

}
