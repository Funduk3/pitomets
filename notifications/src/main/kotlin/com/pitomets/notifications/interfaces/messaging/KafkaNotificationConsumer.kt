package com.pitomets.notifications.interfaces.messaging

import com.pitomets.notifications.application.usecase.SendNotificationUseCase
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import com.pitomets.notifications.interfaces.messaging.mapping.NotificationMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaNotificationConsumer(
    private val useCase: SendNotificationUseCase
) {

    @KafkaListener(
        topics = ["notification.send"],
        groupId = "notification-service"
    )
    fun consume(event: NotificationRequestedEvent) {
        val command = NotificationMapper.toCommand(event)
        useCase.execute(command)
    }
}
