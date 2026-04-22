package com.pitomets.moderator.infrastructure.kafka

import com.pitomets.moderator.infrastructure.kafka.producer.ModerationResultPublisher
import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import com.pitomets.moderator.interfaces.messaging.event.ModerationOperation
import com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.kafka.core.KafkaTemplate

class ModerationResultPublisherTest {
    @Test
    fun `should publish message with expected key`() {
        val kafkaTemplate = mock<KafkaTemplate<String, ModerationProcessedEvent>>()
        val publisher = ModerationResultPublisher(kafkaTemplate, "moderation.processed")
        val event = ModerationProcessedEvent(
            requestEventId = java.util.UUID.randomUUID(),
            entityType = ModerationEntityType.REVIEW,
            entityId = 99L,
            operation = ModerationOperation.UPDATE,
            status = ModerationStatus.REJECTED
        )

        publisher.publish(event)

        verify(kafkaTemplate).send("moderation.processed", "REVIEW:99", event)
    }
}
