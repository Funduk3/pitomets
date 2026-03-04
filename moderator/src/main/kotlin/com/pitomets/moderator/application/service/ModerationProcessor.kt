package com.pitomets.moderator.application.service

import com.pitomets.moderator.config.ModeriumApiProperties
import com.pitomets.moderator.infrastructure.client.ModeriumClient
import com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.util.Locale
import java.util.UUID

@Service
class ModerationProcessor(
    private val moderiumClient: ModeriumClient,
    private val apiProperties: ModeriumApiProperties
) {
    fun process(event: ModerationRequestedEvent): ModerationProcessedEvent {
        val text = event.textParts
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")

        if (text.isBlank()) {
            return error(event, "No text parts for moderation")
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
                processingTimeMs = response.meta?.processingTimeMs,
                modelVersion = response.meta?.modelVersion
            )
        } catch (ex: RestClientResponseException) {
            log.error(
                "Moderium API returned {} for {}:{}",
                ex.statusCode,
                event.entityType,
                event.entityId,
                ex
            )
            error(event, "Moderium API HTTP error: ${ex.statusCode}")
        } catch (ex: RestClientException) {
            log.error(
                "Moderium API call failed for {}:{}",
                event.entityType,
                event.entityId,
                ex
            )
            error(event, "Moderium API connection error: ${ex.message}")
        } catch (ex: IllegalArgumentException) {
            log.error(
                "Moderation config error for {}:{}",
                event.entityType,
                event.entityId,
                ex
            )
            error(event, ex.message ?: "Moderation config error")
        } catch (ex: Exception) {
            log.error(
                "Unexpected moderation error for {}:{}",
                event.entityType,
                event.entityId,
                ex
            )
            error(event, ex.message ?: "Unexpected moderation error")
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
