package com.pitomets.messenger.unit.service

import com.pitomets.messenger.db.DatabaseFactory
import com.pitomets.messenger.service.MessageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageServiceConstantsTest {

    @Test
    fun `max unread limits keep per chat limit below global limit`() {
        assertTrue(MessageService.MAX_UNREAD_MESSAGE_COUNT_PER_CHAT < MessageService.MAX_UNREAD_MESSAGE_COUNT)
    }

    @Test
    fun `database factory max pool size has expected default`() {
        assertEquals(10, DatabaseFactory.MAX_POOL_SIZE)
    }
}
