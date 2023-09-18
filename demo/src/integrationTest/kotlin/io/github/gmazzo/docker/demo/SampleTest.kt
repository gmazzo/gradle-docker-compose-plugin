package io.github.gmazzo.docker.demo

import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleTest {

    @Test
    fun `container properties are exposed`() {
        val props = System.getProperties().filterKeys { (it as? String)?.startsWith("container.") == true }

        assertEquals<Map<Any, Any>>(mapOf("container.integrationTest-app-1.tcp80" to "127.0.0.1:8080"), props)
    }

    @Test
    fun `fetch content from a container`() {
        val url = URL("http://${System.getProperty("container.integrationTest-app-1.tcp80")}")
        val content = url.openStream().bufferedReader().readText()

        assertTrue(content.startsWith("<!DOCTYPE html>"))
    }

}