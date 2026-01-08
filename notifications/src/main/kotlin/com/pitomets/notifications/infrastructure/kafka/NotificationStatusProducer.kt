package com.pitomets.notifications.infrastructure.kafka

import com.pitomets.notifications.application.event.NotificationSentEvent
import org.springframework.kafka.core.KafkaTemplate

class NotificationStatusProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun sent(event: NotificationSentEvent) {
        kafkaTemplate.send("notification.sent", event)
    }
}
