package com.pitomets.notifications.infrastructure.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.port.NotificationSender
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailNotificationSender(
    private val javaMailSender: JavaMailSender,
    private val objectMapper: ObjectMapper
) : NotificationSender {

    private val logger = LoggerFactory.getLogger(EmailNotificationSender::class.java)

    override fun channel(): Channel = Channel.EMAIL

    override fun send(notification: Notification) {
        try {
            // Вариант 1: Используем Kotlin extension из jackson-module-kotlin
            val payloadMap = objectMapper.readValue<Map<String, Any>>(notification.payload)

            // Вариант 2: Если вариант 1 не работает, используйте явное приведение:
            // val payloadMap: Map<String, Any> = objectMapper.readValue(notification.payload, Map::class.java) as Map<String, Any>

            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            // Предполагаем, что в payload есть email и другие данные
            val recipientEmail = payloadMap["email"] as? String
                ?: throw IllegalArgumentException("Email not found in payload")

            helper.setTo(recipientEmail)
            helper.setSubject(payloadMap["subject"] as? String ?: "Notification")
            helper.setText(payloadMap["content"] as? String ?: "", true)

            javaMailSender.send(message)
            logger.info("Email sent successfully for notification id: ${notification.id}")

        } catch (e: Exception) {
            logger.error("Failed to send email for notification id: ${notification.id}", e)
            throw e
        }
    }
}