package com.pitomets.monolit.kafka.moderation.producer

import com.pitomets.monolit.model.kafka.moderation.ModerationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ModerationPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ModerationRequestedEvent>,
    @Value("\${moderation.kafka.topics.moderation-requested}")
    private val topic: String
) {
    fun publish(event: ModerationRequestedEvent) {
        log.debug(
            "Publishing moderation request {} for {}:{}",
            event.eventId,
            event.entityType,
            event.entityId
        )
        kafkaTemplate.send(topic, "${event.entityType}:${event.entityId}", event)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ModerationPublisher::class.java)
    }
}
