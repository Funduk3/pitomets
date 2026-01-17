package com.pitomets.monolit.kafka.notifications.producer

import com.pitomets.monolit.model.kafka.NotificationSentEvent
import org.springframework.kafka.core.KafkaTemplate

class KafkaNotificationStatusProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun sent(event: NotificationSentEvent) {
        kafkaTemplate.send("notification.sent", event)
    }
}
