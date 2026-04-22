package com.pitomets.notifications.interfaces.messaging.mapping

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.MessageType
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationMapperTest {

    @Test
    fun `should map event to command with case-insensitive enums`() {
        val event = NotificationRequestedEvent(
            eventId = 1L,
            userId = 2L,
            channel = "email",
            messageType = "restore_password",
            payload = "user@example.com token"
        )

        val command = NotificationMapper.toCommand(event)

        assertEquals(event.eventId, command.eventId)
        assertEquals(event.userId, command.userId)
        assertEquals(Channel.EMAIL, command.channel)
        assertEquals(MessageType.RESTORE_PASSWORD, command.messageType)
        assertEquals(event.payload, command.payload)
    }

    @Test
    fun `should throw for unsupported channel`() {
        val event = NotificationRequestedEvent(
            eventId = 10L,
            userId = 20L,
            channel = "fax",
            messageType = "CONFIRM",
            payload = "payload"
        )

        val exception = assertThrows<IllegalArgumentException> {
            NotificationMapper.toCommand(event)
        }

        assertEquals("Unsupported channel: fax", exception.message)
    }

    @Test
    fun `should throw for unsupported messageType`() {
        val event = NotificationRequestedEvent(
            eventId = 10L,
            userId = 20L,
            channel = "EMAIL",
            messageType = "HELLO",
            payload = "payload"
        )

        val exception = assertThrows<IllegalArgumentException> {
            NotificationMapper.toCommand(event)
        }

        assertEquals("Unsupported messageType", exception.message)
    }
}
