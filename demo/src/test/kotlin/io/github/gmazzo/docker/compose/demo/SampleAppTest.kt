package io.github.gmazzo.docker.compose.demo

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import kotlin.test.Test

@SpringBootTest(classes = [SampleApp::class])
class SampleAppTest {

    @Test
    fun `context is initialized`(context: ApplicationContext) {
    }

}
