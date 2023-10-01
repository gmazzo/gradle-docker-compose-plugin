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

        assertEquals(
            setOf(
                "container.integrationTest-proxy-1.host",
                "container.integrationTest-proxy-1.tcp80",
                "container.integrationTest-db-1.host",
                "container.integrationTest-db-1.tcp3306",
            ), props.keys
        )
    }

    @Test
    fun `can connect to db`() {
        val dbHost = System.getProperty("container.integrationTest-db-1.host")
        val dbPort = System.getProperty("container.integrationTest-db-1.tcp3306")

        DriverManager.registerDriver(com.mysql.cj.jdbc.Driver())
        DriverManager.getConnection("jdbc:mysql://$dbHost:$dbPort/", "root", "test").use {
            assertEquals("MySQL", it.metaData.databaseProductName)
            assertEquals("5.7.43", it.metaData.databaseProductVersion)
        }
    }

    @Test
    fun `can fetch content from a proxy`() {
        val proxyHost = System.getProperty("container.integrationTest-proxy-1.host")
        val proxyPort = System.getProperty("container.integrationTest-proxy-1.tcp80")

        URL("http://$proxyHost:$proxyPort").openStream().use {
            assertTrue(it.bufferedReader().readText().startsWith("<!DOCTYPE html>"))
        }
    }

}
