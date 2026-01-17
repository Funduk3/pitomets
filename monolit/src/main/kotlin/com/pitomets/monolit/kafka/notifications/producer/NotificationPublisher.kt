package com.pitomets.monolit.kafka.notifications.producer

import com.pitomets.monolit.model.kafka.NotificationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class NotificationPublisher(
    private val kafkaTemplate: KafkaTemplate<String, NotificationRequestedEvent>,

    @Value("\${kafka.topic.notifications}")
    private val topic: String
) {
    private val log = LoggerFactory.getLogger(NotificationPublisher::class.java)

    fun publish(event: NotificationRequestedEvent) {
        log.debug("Publishing notification event to topic {}: {}", topic, event)
        kafkaTemplate.send(topic, event.eventId.toString(), event)
    }
}
