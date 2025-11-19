package com.pitomets.monolit

import com.pitomets.monolit.integration.BaseContainers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [BaseContainers.Companion.Initializer::class])
class MonolitApplicationTests {

	@Test
	fun contextLoads() {
        // todo
	}

}
