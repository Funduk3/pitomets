package com.pitomets.notifications.infrastructure.email

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.port.NotificationSender
import com.pitomets.notifications.exceptions.FailedToSendNotificationException
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailNotificationSender(
    private val javaMailSender: JavaMailSender,
) : NotificationSender {

    private val logger = LoggerFactory.getLogger(EmailNotificationSender::class.java)

    override fun channel(): Channel = Channel.EMAIL

    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    override fun send(notification: Notification) {
        try {
            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            // Воспринимаем payload как email адрес
            val recipientEmail = notification.payload.trim()

            helper.setTo(recipientEmail)
            helper.setSubject("Welcome!")
            helper.setText("Welcome to our platform!", true)

            // пока не подключается к реальному почтовому ящику, поэтому закоменчено
            // javaMailSender.send(message)
            logger.info("Email sent successfully for notification id: ${notification.id} to $recipientEmail")

        } catch (e: IllegalArgumentException) {
            logger.error("Invalid payload for notification id: ${notification.id}", e)
            throw e
        } catch (e: MailException) {
            logger.error("Mail send failed for notification id: ${notification.id}", e)
            throw FailedToSendNotificationException("Failed to send email", e)
        } catch (e: Exception) {
            logger.error("Unexpected error while sending email for notification id: ${notification.id}", e)
            throw FailedToSendNotificationException("Unexpected error while sending email", e)
        }
    }
}
