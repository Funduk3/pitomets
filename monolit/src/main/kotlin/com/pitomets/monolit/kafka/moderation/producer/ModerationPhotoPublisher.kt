package com.pitomets.monolit.kafka.moderation.producer

import com.pitomets.monolit.model.kafka.moderation.ModerationPhotoRequestedEvent
import com.pitomets.monolit.model.kafka.moderation.ModerationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ModerationPhotoPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ModerationPhotoRequestedEvent>,
    @Value("\${moderation.kafka.topics.moderation-requested-photo}")
    private val topic: String
) {
    fun publish(event: ModerationPhotoRequestedEvent) {
        log.debug(
            "Publishing moderation photo event {} with URI {}",
            event.eventId,
            event.photoURI,
        )
        kafkaTemplate.send(topic, "photo", event)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ModerationPhotoPublisher::class.java)
    }
}
