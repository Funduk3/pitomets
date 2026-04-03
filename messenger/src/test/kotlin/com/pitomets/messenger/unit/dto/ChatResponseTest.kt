package com.pitomets.messenger.unit.dto

import com.pitomets.messenger.dto.ChatResponse
import com.pitomets.messenger.dto.MessageResponse
import com.pitomets.messenger.models.ChatEntity
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChatResponseTest {

    @Test
    fun `from maps ChatEntity with provided optional fields`() {
        val entity = ChatEntity(
            id = 7L,
            user1Id = 100L,
            user2Id = 200L,
            listingId = 33L,
            listingTitle = "Щенки",
            createdAt = Instant.parse("2026-04-03T09:00:00Z"),
            updatedAt = Instant.parse("2026-04-03T09:30:00Z"),
            unreadCountUser1 = 0,
            unreadCountUser2 = 2,
            lastUnreadMessageIdUser1 = null,
            lastUnreadMessageIdUser2 = 77L
        )
        val lastMessage = MessageResponse(
            id = 77L,
            chatId = 7L,
            senderId = 200L,
            content = "Привет",
            createdAt = "2026-04-03T09:29:00Z",
            isRead = false
        )

        val response = ChatResponse.from(entity, unreadCount = 2, lastMessage = lastMessage)

        assertEquals(7L, response.id)
        assertEquals(100L, response.user1Id)
        assertEquals(200L, response.user2Id)
        assertEquals(33L, response.listingId)
        assertEquals("Щенки", response.listingTitle)
        assertEquals("2026-04-03T09:00:00Z", response.createdAt)
        assertEquals("2026-04-03T09:30:00Z", response.updatedAt)
        assertEquals(2, response.unreadCount)
        assertEquals(lastMessage, response.lastMessage)
        assertNull(response.lastUnreadMessage)
    }

    @Test
    fun `from uses defaults when optional values are omitted`() {
        val entity = ChatEntity(
            id = 8L,
            user1Id = 1L,
            user2Id = 2L,
            listingId = null,
            listingTitle = null,
            createdAt = Instant.parse("2026-04-03T10:00:00Z"),
            updatedAt = Instant.parse("2026-04-03T10:01:00Z"),
            unreadCountUser1 = 0,
            unreadCountUser2 = 0,
            lastUnreadMessageIdUser1 = null,
            lastUnreadMessageIdUser2 = null
        )

        val response = ChatResponse.from(entity)

        assertEquals(0, response.unreadCount)
        assertNull(response.lastMessage)
        assertNull(response.lastUnreadMessage)
    }
}
