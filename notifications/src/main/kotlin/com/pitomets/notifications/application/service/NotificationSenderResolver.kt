package com.pitomets.notifications.application.service

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.port.NotificationSender

class NotificationSenderResolver(
    senders: List<NotificationSender>
) {
    private val senderMap = senders.associateBy { it.channel() }

    fun resolve(channel: Channel): NotificationSender =
        senderMap[channel]
            ?: throw IllegalStateException("No sender for $channel")
}
