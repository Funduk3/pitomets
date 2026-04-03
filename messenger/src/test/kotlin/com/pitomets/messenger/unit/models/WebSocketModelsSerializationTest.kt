package com.pitomets.messenger.unit.models

import com.pitomets.messenger.dto.MessageResponse
import com.pitomets.messenger.models.ReadReceiptEvent
import com.pitomets.messenger.models.SyncResponse
import com.pitomets.messenger.models.WebSocketMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebSocketModelsSerializationTest {

    private val json = Json

    @Test
    fun `WebSocketMessage is deserialized with map payload`() {
        val raw = """
            {
              "type": "sync",
              "lastMessageIds": {
                "1": "10",
                "2": "20"
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString(WebSocketMessage.serializer(), raw)

        assertEquals("sync", parsed.type)
        assertEquals(mapOf("1" to "10", "2" to "20"), parsed.lastMessageIds)
    }

    @Test
    fun `ReadReceiptEvent is serialized into expected JSON`() {
        val event = ReadReceiptEvent(type = "read_receipt", chatId = 55L, readerId = 100L)

        val raw = json.encodeToString(event)

        assertTrue(raw.contains("\"type\":\"read_receipt\""))
        assertTrue(raw.contains("\"chatId\":55"))
        assertTrue(raw.contains("\"readerId\":100"))
    }

    @Test
    fun `SyncResponse is serialized with nested messages map`() {
        val response = SyncResponse(
            type = "sync_response",
            messages = mapOf(
                "10" to listOf(
                    MessageResponse(
                        id = 1,
                        chatId = 10,
                        senderId = 2,
                        content = "Hi",
                        createdAt = "2026-04-03T10:20:00Z",
                        isRead = false
                    )
                )
            )
        )

        val raw = json.encodeToString(response)

        assertTrue(raw.contains("\"type\":\"sync_response\""))
        assertTrue(raw.contains("\"10\""))
        assertTrue(raw.contains("\"content\":\"Hi\""))
    }
}
