package com.pitomets.moderator.infrastructure.kafka.consumer

import com.pitomets.moderator.application.service.ModerationProcessor
import com.pitomets.moderator.infrastructure.kafka.producer.ModerationPhotoPublisher
import com.pitomets.moderator.interfaces.messaging.event.photo.ModerationPhotoRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaModerationPhotoConsumer(
    private val moderationProcessor: ModerationProcessor,
    private val moderationPhotoPublisher: ModerationPhotoPublisher,
) {
    @KafkaListener(
        topics = ["\${moderator.kafka.topics.moderation-requested-photo}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun consume(event: ModerationPhotoRequestedEvent) {
        log.debug(
            "Received moderation photo event {} with URI {}",
            event.eventId,
            event.photoURI,
        )
        val processed = moderationProcessor.processPhoto(event)
        moderationPhotoPublisher.publish(processed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaModerationPhotoConsumer::class.java)
    }
}
