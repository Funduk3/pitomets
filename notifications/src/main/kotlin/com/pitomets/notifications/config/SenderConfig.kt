package com.pitomets.notifications.config

import com.pitomets.notifications.application.service.NotificationSenderResolver
import com.pitomets.notifications.domain.port.NotificationSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SenderConfig {

    @Bean
    fun notificationSenderResolver(
        senders: List<NotificationSender>
    ): NotificationSenderResolver =
        NotificationSenderResolver(senders)
}
