package com.pitomets.messenger.unit.dto

import com.pitomets.messenger.dto.MessageResponse
import com.pitomets.messenger.models.MessageEntity
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageResponseTest {

    @Test
    fun `from maps MessageEntity fields to DTO`() {
        val entity = MessageEntity(
            id = 1L,
            chatId = 10L,
            senderId = 99L,
            content = "hello",
            createdAt = Instant.parse("2026-04-03T08:15:30Z"),
            isRead = true
        )

        val response = MessageResponse.from(entity)

        assertEquals(1L, response.id)
        assertEquals(10L, response.chatId)
        assertEquals(99L, response.senderId)
        assertEquals("hello", response.content)
        assertEquals("2026-04-03T08:15:30Z", response.createdAt)
        assertEquals(true, response.isRead)
    }
}
