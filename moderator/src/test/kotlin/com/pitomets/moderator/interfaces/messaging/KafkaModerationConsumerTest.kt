package com.pitomets.moderator.interfaces.messaging

import com.pitomets.moderator.application.service.ModerationProcessor
import com.pitomets.moderator.infrastructure.kafka.consumer.KafkaModerationConsumer
import com.pitomets.moderator.infrastructure.kafka.producer.ModerationResultPublisher
import com.pitomets.moderator.interfaces.messaging.event.ModerationEntityType
import com.pitomets.moderator.interfaces.messaging.event.ModerationOperation
import com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import com.pitomets.moderator.interfaces.messaging.event.ModerationStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KafkaModerationConsumerTest {
    @Test
    fun `should process incoming event and publish processed result`() {
        val processor = mock<ModerationProcessor>()
        val publisher = mock<ModerationResultPublisher>()
        val consumer = KafkaModerationConsumer(processor, publisher)
        val request = ModerationRequestedEvent(
            entityType = ModerationEntityType.LISTING,
            entityId = 7L,
            operation = ModerationOperation.CREATE,
            textParts = listOf("title")
        )
        val processed = ModerationProcessedEvent(
            requestEventId = request.eventId,
            entityType = request.entityType,
            entityId = request.entityId,
            operation = request.operation,
            status = ModerationStatus.APPROVED
        )
        whenever(processor.process(request)).thenReturn(processed)

        consumer.consume(request)

        verify(processor).process(request)
        verify(publisher).publish(processed)
    }
}
