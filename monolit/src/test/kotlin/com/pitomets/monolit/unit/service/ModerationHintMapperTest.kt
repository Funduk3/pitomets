package com.pitomets.monolit.unit.service

import com.pitomets.monolit.service.moderationHint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModerationHintMapperTest {

    @Test
    fun `returns null when all fields are absent`() {
        val result = moderationHint(
            status = null,
            reason = null,
            toxicityScore = null,
            profanityDetected = null,
            sexualContentDetected = null,
            sourceAction = null,
            modelVersion = null
        )

        assertNull(result)
    }

    @Test
    fun `returns DTO when at least one field is present`() {
        val result = moderationHint(
            status = "REJECTED",
            reason = "toxic",
            toxicityScore = 0.92,
            profanityDetected = true,
            sexualContentDetected = false,
            sourceAction = "reject",
            modelVersion = "v1"
        )

        requireNotNull(result)
        assertEquals("REJECTED", result.status)
        assertEquals("toxic", result.reason)
        assertEquals(0.92, result.toxicityScore)
        assertEquals(true, result.profanityDetected)
        assertEquals(false, result.sexualContentDetected)
        assertEquals("reject", result.sourceAction)
        assertEquals("v1", result.modelVersion)
    }

    @Test
    fun `returns DTO when only model version is present`() {
        val result = moderationHint(
            status = null,
            reason = null,
            toxicityScore = null,
            profanityDetected = null,
            sexualContentDetected = null,
            sourceAction = null,
            modelVersion = "v2"
        )

        requireNotNull(result)
        assertEquals("v2", result.modelVersion)
        assertNull(result.status)
    }
}
