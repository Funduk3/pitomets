package com.pitomets.moderator.application.service

import com.pitomets.moderator.config.ModeriumApiProperties
import com.pitomets.moderator.infrastructure.client.ModeriumClient
import com.pitomets.moderator.infrastructure.dto.ModeriumAnalyzeResponse
import com.pitomets.moderator.infrastructure.dto.ModeriumDecision
import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import com.pitomets.moderator.interfaces.messaging.event.ModerationOperation
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModerationProcessorTest {
    @Test
    fun `should map reject decision to rejected status`() {
        val client = StubClient(
            response = ModeriumAnalyzeResponse(
                decision = ModeriumDecision(action = "reject", reason = "bad words")
            )
        )
        val processor = ModerationProcessor(client, ModeriumApiProperties(mode = "strong"))

        val result = processor.process(
            ModerationRequestedEvent(
                entityType = ModerationEntityType.LISTING,
                entityId = 42L,
                operation = ModerationOperation.CREATE,
                textParts = listOf("title", "description")
            )
        )

        assertEquals(ModerationStatus.REJECTED, result.status)
        assertEquals("bad words", result.reason)
    }

    @Test
    fun `should return error when message has no text`() {
        val client = StubClient(
            response = ModeriumAnalyzeResponse(
                decision = ModeriumDecision(action = "approve")
            )
        )
        val processor = ModerationProcessor(client, ModeriumApiProperties(mode = "strong"))

        val result = processor.process(
            ModerationRequestedEvent(
                entityType = ModerationEntityType.REVIEW,
                entityId = 1L,
                operation = ModerationOperation.UPDATE,
                textParts = listOf("   ", "")
            )
        )

        assertEquals(ModerationStatus.ERROR, result.status)
        assertTrue(result.reason?.contains("No text parts") == true)
        assertEquals(0, client.calls)
    }

    private class StubClient(
        private val response: ModeriumAnalyzeResponse
    ) : ModeriumClient {
        var calls: Int = 0

        override fun analyze(
            text: String,
            mode: String,
            withAnimal: Boolean
        ): ModeriumAnalyzeResponse {
            calls += 1
            return response
        }
    }
}
