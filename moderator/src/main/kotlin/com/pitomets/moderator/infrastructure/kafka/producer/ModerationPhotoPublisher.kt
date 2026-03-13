package com.pitomets.moderator.infrastructure.kafka.producer

import com.pitomets.moderator.interfaces.messaging.event.photo.ModerationPhotoProcessedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ModerationPhotoPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ModerationPhotoProcessedEvent>,
    @Value("\${moderator.kafka.topics.moderation-processed-photo}")
    private val topic: String,
) {
    fun publish(event: ModerationPhotoProcessedEvent) {
        log.debug(
            "Publishing moderation photo result for {}, status={}",
            event.eventId,
            event.status
        )
        kafkaTemplate.send(topic, "PHOTO", event)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ModerationPhotoPublisher::class.java)
    }
}