package com.pitomets.monolit.testContainers

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestKafkaTopicsConfig {
    @Bean
    fun notificationSendTopic(): NewTopic = NewTopic("notification.send", 1, 1.toShort())

    @Bean
    fun notificationSentTopic(): NewTopic = NewTopic("notification.sent", 1, 1.toShort())

    @Bean
    fun notificationFailedTopic(): NewTopic = NewTopic("notification.failed", 1, 1.toShort())
}
