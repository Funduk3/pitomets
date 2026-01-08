package com.pitomets.notifications.domain.port

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Notification

interface NotificationSender {
    fun channel(): Channel
    fun send(notification: Notification)
}

