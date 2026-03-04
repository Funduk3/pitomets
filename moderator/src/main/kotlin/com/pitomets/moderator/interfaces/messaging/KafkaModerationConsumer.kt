package com.pitomets.moderator.interfaces.messaging

import com.pitomets.moderator.application.service.ModerationProcessor
import com.pitomets.moderator.infrastructure.kafka.ModerationResultPublisher
import com.pitomets.moderator.interfaces.messaging.event.ModerationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaModerationConsumer(
    private val moderationProcessor: ModerationProcessor,
    private val moderationResultPublisher: ModerationResultPublisher
) {
    @KafkaListener(
        topics = ["\${moderator.kafka.topics.moderation-requested}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun consume(event: ModerationRequestedEvent) {
        log.debug(
            "Received moderation event {} for {}:{}",
            event.eventId,
            event.entityType,
            event.entityId
        )
        val processed = moderationProcessor.process(event)
        moderationResultPublisher.publish(processed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaModerationConsumer::class.java)
    }
}
