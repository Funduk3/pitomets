package com.pitomets.moderator.application.service

import com.pitomets.moderator.config.ModeriumApiProperties
import com.pitomets.moderator.infrastructure.client.HttpModeriumPhotoClient
import com.pitomets.moderator.infrastructure.client.ModeriumClient
import com.pitomets.moderator.infrastructure.dto.ModeriumAnalyzeResponse
import com.pitomets.moderator.infrastructure.dto.ModeriumCategories
import com.pitomets.moderator.infrastructure.dto.ModeriumCategoryMatches
import com.pitomets.moderator.infrastructure.dto.ModeriumDecision
import com.pitomets.moderator.infrastructure.dto.ModeriumMeta
import com.pitomets.moderator.infrastructure.dto.ModeriumToxicity
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.text.ModeriumAnalyzeResponse
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumDecision
import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import com.pitomets.moderator.interfaces.messaging.event.ModerationOperation
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        val props = ModeriumApiProperties(baseUrl = "http://localhost", token = "test", mode = "strong")
        val processor = ModerationProcessor(client, HttpModeriumPhotoClient(props), props)

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
        val props = ModeriumApiProperties(baseUrl = "http://localhost", token = "test", mode = "strong")
        val processor = ModerationProcessor(client, HttpModeriumPhotoClient(props), props)

        val result = processor.process(
            ModerationRequestedEvent(
                entityType = ModerationEntityType.REVIEW,
                entityId = 1L,
                operation = ModerationOperation.UPDATE,
                textParts = listOf("   ", "")
            )
        )

        assertEquals(ModerationStatus.ERROR, result.status)
        assertEquals(0, client.calls)
    }

    @Test
    fun `should map approve aliases to approved status`() {
        val processor = ModerationProcessor(
            StubClient(ModeriumAnalyzeResponse(decision = ModeriumDecision(action = "allow"))),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.APPROVED, result.status)
    }

    @Test
    fun `should map review aliases to review status`() {
        val processor = ModerationProcessor(
            StubClient(ModeriumAnalyzeResponse(decision = ModeriumDecision(action = "manual_review"))),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.REVIEW, result.status)
    }

    @Test
    fun `should map unknown action to error status and keep null source action when blank`() {
        val processor = ModerationProcessor(
            StubClient(ModeriumAnalyzeResponse(decision = ModeriumDecision(action = ""))),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.ERROR, result.status)
        assertNull(result.sourceAction)
    }

    @Test
    fun `should fallback to properties withAnimal when event value is null`() {
        val client = StubClient(ModeriumAnalyzeResponse(decision = ModeriumDecision(action = "approve")))
        val processor = ModerationProcessor(client, ModeriumApiProperties(mode = "strict", withAnimal = false))

        processor.process(defaultEvent(withAnimal = null))

        assertEquals(false, client.lastWithAnimal)
        assertEquals("strict", client.lastMode)
        assertEquals("title\ndescription", client.lastText)
    }

    @Test
    fun `should use event withAnimal when provided`() {
        val client = StubClient(ModeriumAnalyzeResponse(decision = ModeriumDecision(action = "approve")))
        val processor = ModerationProcessor(client, ModeriumApiProperties(mode = "strong", withAnimal = false))

        processor.process(defaultEvent(withAnimal = true))

        assertEquals(true, client.lastWithAnimal)
    }

    @Test
    fun `should map known technical errors to localized text`() {
        val processor = ModerationProcessor(
            ThrowingClient(RuntimeException("connect timed out")),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.ERROR, result.status)
        assertEquals("Сервис MODERIUM временно недоступен", result.reason)
    }

    @Test
    fun `should map blank exception message to default localized error`() {
        val processor = ModerationProcessor(
            ThrowingClient(RuntimeException("   ")),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.ERROR, result.status)
        assertEquals("Неожиданная ошибка модерации", result.reason)
    }

    @Test
    fun `should keep unknown error reason as is`() {
        val processor = ModerationProcessor(
            ThrowingClient(RuntimeException("custom upstream fail")),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.ERROR, result.status)
        assertEquals("custom upstream fail", result.reason)
    }

    @Test
    fun `should map known token message to localized reason`() {
        val processor = ModerationProcessor(
            ThrowingClient(IllegalArgumentException("MODERIUM_API_TOKEN is empty")),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(ModerationStatus.ERROR, result.status)
        assertEquals("Токен MODERIUM API не настроен", result.reason)
    }

    @Test
    fun `should include optional fields from response`() {
        val processor = ModerationProcessor(
            StubClient(
                ModeriumAnalyzeResponse(
                    decision = ModeriumDecision(action = "reject", reason = "bad words"),
                    categories = ModeriumCategories(
                        profanity = ModeriumCategoryMatches(detected = true, matches = listOf("bad")),
                        sexualContent = ModeriumCategoryMatches(detected = false),
                        toxicity = ModeriumToxicity(score = 0.81)
                    ),
                    meta = ModeriumMeta(
                        processingTimeMs = 125L,
                        modelVersion = "v2"
                    )
                )
            ),
            ModeriumApiProperties(mode = "strong")
        )

        val result = processor.process(defaultEvent())

        assertEquals(0.81, result.toxicityScore)
        assertEquals(true, result.profanityDetected)
        assertEquals(false, result.sexualContentDetected)
        assertEquals(125L, result.processingTimeMs)
        assertEquals("v2", result.modelVersion)
    }

    private fun defaultEvent(withAnimal: Boolean? = null): ModerationRequestedEvent =
        ModerationRequestedEvent(
            entityType = ModerationEntityType.LISTING,
            entityId = 42L,
            operation = ModerationOperation.CREATE,
            textParts = listOf(" title ", "description", " "),
            withAnimal = withAnimal
        )

    private class StubClient(
        private val response: ModeriumAnalyzeResponse
    ) : ModeriumClient {
        var calls: Int = 0
        var lastText: String? = null
        var lastMode: String? = null
        var lastWithAnimal: Boolean? = null

        override fun analyze(
            text: String,
            mode: String,
            withAnimal: Boolean
        ): ModeriumAnalyzeResponse {
            calls += 1
            lastText = text
            lastMode = mode
            lastWithAnimal = withAnimal
            return response
        }
    }

    private class ThrowingClient(
        private val throwable: Throwable
    ) : ModeriumClient {
        override fun analyze(
            text: String,
            mode: String,
            withAnimal: Boolean
        ): ModeriumAnalyzeResponse {
            throw throwable
        }
    }
}
