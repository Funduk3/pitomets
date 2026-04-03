package com.pitomets.messenger.unit.models

import com.pitomets.messenger.dto.MessageResponse
import com.pitomets.messenger.models.BlockStatusEvent
import com.pitomets.messenger.models.ReadReceiptEvent
import com.pitomets.messenger.models.SyncResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockAndSyncModelsTest {

    private val json = Json

    @Test
    fun `BlockStatusEvent uses default type and is serialized`() {
        val event = BlockStatusEvent(
            otherUserId = 42,
            blockedByMe = true,
            blockedMe = false,
            blockedAny = true
        )

        val raw = json.encodeToString(event)

        val parsed = json.decodeFromString(BlockStatusEvent.serializer(), raw)

        assertEquals("block_status", parsed.type)
        assertEquals(42L, parsed.otherUserId)
        assertTrue(parsed.blockedByMe)
        assertFalse(parsed.blockedMe)
        assertTrue(parsed.blockedAny)
    }

    @Test
    fun `BlockStatusEvent is deserialized from json`() {
        val raw = """
            {
              "type": "block_status",
              "otherUserId": 10,
              "blockedByMe": false,
              "blockedMe": true,
              "blockedAny": true
            }
        """.trimIndent()

        val parsed = json.decodeFromString(BlockStatusEvent.serializer(), raw)

        assertEquals("block_status", parsed.type)
        assertEquals(10L, parsed.otherUserId)
        assertFalse(parsed.blockedByMe)
        assertTrue(parsed.blockedMe)
        assertTrue(parsed.blockedAny)
    }

    @Test
    fun `ReadReceiptEvent is deserialized from json`() {
        val raw = """
            {
              "type": "read_receipt",
              "chatId": 77,
              "readerId": 501
            }
        """.trimIndent()

        val parsed = json.decodeFromString(ReadReceiptEvent.serializer(), raw)

        assertEquals("read_receipt", parsed.type)
        assertEquals(77L, parsed.chatId)
        assertEquals(501L, parsed.readerId)
    }

    @Test
    fun `SyncResponse is deserialized with nested messages`() {
        val raw = """
            {
              "type": "sync_response",
              "messages": {
                "9": [
                  {
                    "id": 1,
                    "chatId": 9,
                    "senderId": 11,
                    "content": "hello",
                    "createdAt": "2026-04-03T12:00:00Z",
                    "isRead": false
                  }
                ]
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString(SyncResponse.serializer(), raw)

        assertEquals("sync_response", parsed.type)
        val msg: MessageResponse = parsed.messages.getValue("9").single()
        assertEquals(1L, msg.id)
        assertEquals("hello", msg.content)
        assertFalse(msg.isRead)
    }
}
