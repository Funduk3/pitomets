package com.pitomets.moderator.application.service

import com.pitomets.moderator.config.ModeriumApiProperties
import com.pitomets.moderator.infrastructure.client.ModeriumClient
import com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.UUID

@Service
class ModerationProcessor(
    private val moderiumClient: ModeriumClient,
    private val apiProperties: ModeriumApiProperties
) {
    @Suppress("TooGenericExceptionCaught")
    fun process(event: ModerationRequestedEvent): ModerationProcessedEvent {
        val text = event.textParts
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")

        if (text.isBlank()) {
            return error(event, "Отсутствует текст для модерации")
        }

        return try {
            val response = moderiumClient.analyze(
                text = text,
                mode = apiProperties.mode,
                withAnimal = event.withAnimal ?: apiProperties.withAnimal
            )

            val action = response.decision?.action.orEmpty()
            val status = mapActionToStatus(action)

            ModerationProcessedEvent(
                requestEventId = event.eventId,
                entityType = event.entityType,
                entityId = event.entityId,
                operation = event.operation,
                status = status,
                reason = response.decision?.reason,
                sourceAction = action.ifBlank { null },
                toxicityScore = response.categories?.toxicity?.score,
                profanityDetected = response.categories?.profanity?.detected,
                sexualContentDetected = response.categories?.sexualContent?.detected,
                processingTimeMs = response.meta?.processingTimeMs,
                modelVersion = response.meta?.modelVersion
            )
        } catch (ex: Exception) {
            log.error(
                "Unexpected moderation error for {}:{}",
                event.entityType,
                event.entityId,
                ex
            )
            error(event, localizeErrorReason(ex.message))
        }
    }

    private fun localizeErrorReason(reason: String?): String {
        val message = reason?.trim().orEmpty()
        if (message.isBlank()) {
            return "Неожиданная ошибка модерации"
        }

        val normalized = message.lowercase(Locale.getDefault())
        return when {
            normalized.contains("moderium_api_token is empty") ->
                "Токен MODERIUM API не настроен"
            normalized.contains("no text parts for moderation") ->
                "Отсутствует текст для модерации"
            normalized.contains("i/o error") ||
                normalized.contains("connection refused") ||
                normalized.contains("connect timed out") ||
                normalized.contains("read timed out") ->
                "Сервис MODERIUM временно недоступен"
            normalized.contains("unexpected moderation error") ->
                "Неожиданная ошибка модерации"
            else -> message
        }
    }

    private fun mapActionToStatus(action: String): ModerationStatus {
        return when (action.lowercase(Locale.getDefault())) {
            "approve", "allow", "accept", "ok" -> ModerationStatus.APPROVED
            "reject", "deny", "block" -> ModerationStatus.REJECTED
            "review", "manual_review", "manual-review", "needs_review" -> ModerationStatus.REVIEW
            else -> ModerationStatus.ERROR
        }
    }

    private fun error(event: ModerationRequestedEvent, reason: String): ModerationProcessedEvent =
        ModerationProcessedEvent(
            eventId = UUID.randomUUID(),
            requestEventId = event.eventId,
            entityType = event.entityType,
            entityId = event.entityId,
            operation = event.operation,
            status = ModerationStatus.ERROR,
            reason = reason
        )

    companion object {
        private val log = LoggerFactory.getLogger(ModerationProcessor::class.java)
    }
}
