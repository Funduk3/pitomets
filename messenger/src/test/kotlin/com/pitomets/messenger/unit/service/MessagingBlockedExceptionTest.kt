package com.pitomets.messenger.unit.service

import com.pitomets.messenger.service.MessagingBlockedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessagingBlockedExceptionTest {

    @Test
    fun `MessagingBlockedException is a RuntimeException with message`() {
        val ex = MessagingBlockedException("blocked")
        assertEquals("blocked", ex.message)
    }
}
