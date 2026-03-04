package com.pitomets.moderator.infrastructure.kafka

import com.pitomets.moderator.interfaces.messaging.event.ModerationProcessedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ModerationResultPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ModerationProcessedEvent>,
    @Value("\${moderator.kafka.topics.moderation-processed}")
    private val topic: String
) {
    fun publish(event: ModerationProcessedEvent) {
        log.debug(
            "Publishing moderation result for {}:{} status={}",
            event.entityType,
            event.entityId,
            event.status
        )
        kafkaTemplate.send(topic, "${event.entityType}:${event.entityId}", event)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ModerationResultPublisher::class.java)
    }
}
