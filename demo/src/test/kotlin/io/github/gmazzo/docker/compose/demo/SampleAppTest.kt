package io.github.gmazzo.docker.compose.demo

import kotlin.test.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest(classes = [SampleApp::class])
class SampleAppTest {

    @Test
    fun `context is initialized`(context: ApplicationContext) {
    }

}
