package com.pitomets.notifications.infrastructure.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.port.NotificationSender
import com.pitomets.notifications.exceptions.FailedToSendNotificationException
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
            // Парсим payload в Map
            val payloadMap: Map<String, Any> = objectMapper.readValue(notification.payload)

            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            val recipientEmail = (payloadMap["email"] ?: payloadMap["recipient"]) as? String
                ?: throw IllegalArgumentException("Recipient email not found in payload")

            helper.setTo(recipientEmail)
            helper.setSubject(payloadMap["subject"] as? String ?: "Notification")
            helper.setText(
                (payloadMap["content"] ?: payloadMap["body"]) as? String ?: "",
                true
            )

            javaMailSender.send(message)
            logger.info("Email sent successfully for notification id: ${notification.id}")
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid payload for notification id: ${notification.id}", e)
            throw e
        } catch (e: FailedToSendNotificationException) {
            logger.error("Failed to send email for notification id: ${notification.id}", e)
            throw e
        }
    }
}
