package com.pitomets.messenger.unit.dto

import com.pitomets.messenger.dto.CreateChatRequest
import com.pitomets.messenger.dto.CreateMessageRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateRequestsSerializationTest {

    private val json = Json

    @Test
    fun `CreateChatRequest supports optional listing fields`() {
        val request = CreateChatRequest(userId = 100, listingId = 5, listingTitle = "Щенки")

        val raw = json.encodeToString(request)
        val decoded = json.decodeFromString(CreateChatRequest.serializer(), raw)

        assertTrue(raw.contains("\"userId\":100"))
        assertEquals(100L, decoded.userId)
        assertEquals(5L, decoded.listingId)
        assertEquals("Щенки", decoded.listingTitle)
    }

    @Test
    fun `CreateChatRequest keeps null defaults when listing fields are missing`() {
        val raw = """
            {
              "userId": 300
            }
        """.trimIndent()

        val decoded = json.decodeFromString(CreateChatRequest.serializer(), raw)

        assertEquals(300L, decoded.userId)
        assertNull(decoded.listingId)
        assertNull(decoded.listingTitle)
    }

    @Test
    fun `CreateMessageRequest round trip keeps chatId and content`() {
        val request = CreateMessageRequest(chatId = 77, content = "test message")

        val raw = json.encodeToString(request)
        val decoded = json.decodeFromString(CreateMessageRequest.serializer(), raw)

        assertTrue(raw.contains("\"chatId\":77"))
        assertTrue(raw.contains("\"content\":\"test message\""))
        assertEquals(77L, decoded.chatId)
        assertEquals("test message", decoded.content)
    }
}
