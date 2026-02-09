package com.pitomets.notifications.infrastructure.email

import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.MessageType
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
            val parts = notification.payload.trim().split(" ")
            val typeName = parts[0]
            val email = parts[1]
            val token = parts.getOrNull(2)

            val type = MessageType.entries.firstOrNull {
                it.name.equals(typeName, ignoreCase = true)
            }

            when (type) {
                MessageType.REGISTRATION -> sendAfterRegistration(email)
                MessageType.CONFIRM -> sendToConfirm(email, token)
                MessageType.RESTORE_PASSWORD -> sendRestorePassword(email, token)
                else -> {
                    throw IllegalArgumentException("Unsupported message type $typeName")
                }
            }
            logger.info("Email sent successfully for notification id: ${notification.id} to $email")

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

    fun sendAfterRegistration(email: String) {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setTo(email)
        helper.setSubject("Thank you for registration!")
        helper.setText("Welcome to pitomets", true)
        javaMailSender.send(message)
    }

    fun sendToConfirm(email: String, token: String?) {
        if (token == null) {
            logger.warn("No token provided for confirmation email to $email")
            return
        }
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setTo(email)
        helper.setSubject("Please confirm your email")
        val link = "http://localhost:5173/confirm?token=$token"
        helper.setText("<h1>Confirm your email</h1><p>Click the link below to confirm your email:</p><a href=\"$link\">Confirm Email</a>", true)
        javaMailSender.send(message)
    }

    fun sendRestorePassword(email: String, token: String?) {
        if (token == null) {
            logger.warn("No token provided for password reset email to $email")
            return
        }
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setTo(email)
        helper.setSubject("Password Reset Request")
        val link = "http://localhost:5173/reset-password?token=$token"
        helper.setText("<h1>Reset Your Password</h1><p>Click the link below to reset your password:</p><a href=\"$link\">Reset Password</a>", true)
        javaMailSender.send(message)
    }
}
