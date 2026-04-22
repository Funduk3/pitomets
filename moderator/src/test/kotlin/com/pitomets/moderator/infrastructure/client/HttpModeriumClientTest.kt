package com.pitomets.moderator.infrastructure.client

import com.pitomets.moderator.config.ModeriumApiProperties
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HttpModeriumClientTest {
    @Test
    fun `should call moderium api with expected payload and token`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "decision": {"action":"reject","reason":"bad words"},
                          "categories": {"toxicity": {"score": 0.91}},
                          "meta": {"processing_time_ms": 45, "model_version":"v1"}
                        }
                        """.trimIndent()
                    )
                    .addHeader("Content-Type", "application/json")
            )

            val client = HttpModeriumClient(
                ModeriumApiProperties(
                    baseUrl = server.url("/").toString(),
                    token = "test-token"
                )
            )

            val response = client.analyze(
                text = "sample text",
                mode = "strong",
                withAnimal = true
            )

            val recorded = server.takeRequest()
            assertEquals("/api/v1/analyze", recorded.path)
            assertEquals("POST", recorded.method)
            assertEquals("test-token", recorded.getHeader("X-API-Token"))
            assertEquals("reject", response.decision?.action)
            assertEquals(0.91, response.categories?.toxicity?.score)
            assertEquals(45L, response.meta?.processingTimeMs)
        }
    }

    @Test
    fun `should fail when api token is empty`() {
        val client = HttpModeriumClient(
            ModeriumApiProperties(baseUrl = "http://localhost:9999", token = "")
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            client.analyze("text", "strong", true)
        }

        assertEquals("MODERIUM_API_TOKEN is empty", error.message)
    }

    @Test
    fun `should fail when api returns empty body`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val client = HttpModeriumClient(
                ModeriumApiProperties(
                    baseUrl = server.url("/").toString(),
                    token = "token"
                )
            )

            val error = assertThrows(IllegalStateException::class.java) {
                client.analyze("text", "strong", false)
            }

            assertEquals("Moderium API returned empty body", error.message)
        }
    }
}
