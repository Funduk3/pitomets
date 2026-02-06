package com.pitomets.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import com.pitomets.notifications.infrastructure.email.EmailNotificationSender
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.exceptions.FailedToSendNotificationException
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties

class EmailNotificationSenderTest {

    private val mailSender: JavaMailSender = mock()
    private val objectMapper: ObjectMapper = ObjectMapper()
    private lateinit var sender: EmailNotificationSender

    @BeforeEach
    fun setUp() {
        sender = EmailNotificationSender(mailSender, objectMapper)
        val session = Session.getInstance(Properties())
        whenever(mailSender.createMimeMessage()).thenReturn(MimeMessage(session))
    }

    @Test
    fun `send should send email for valid payload`() {
        val payload = objectMapper.writeValueAsString(mapOf(
            "subject" to "Hi",
            "body" to "Hello",
            "recipient" to "user@example.com"
        ))

        val notification = Notification(
            id = 1L,
            eventId = 123L,
            userId = 1L,
            channel = Channel.EMAIL,
            payload = payload,
            status = com.pitomets.notifications.domain.model.Status.NEW
        )

        sender.send(notification)

        verify(mailSender).send(any<MimeMessage>())
    }

    @Test
    fun `send should throw IllegalArgumentException for invalid payload`() {
        val payload = objectMapper.writeValueAsString(mapOf(
            "subject" to "Hi",
            "body" to "Hello"
            // missing recipient
        ))

        val notification = Notification(
            id = 2L,
            eventId = 124L,
            userId = 2L,
            channel = Channel.EMAIL,
            payload = payload,
            status = com.pitomets.notifications.domain.model.Status.NEW
        )

        assertThrows(IllegalArgumentException::class.java) {
            sender.send(notification)
        }
    }

    @Test
    fun `send should wrap MailException into FailedToSendNotificationException`() {
        val payload = objectMapper.writeValueAsString(mapOf(
            "subject" to "Hi",
            "body" to "Hello",
            "recipient" to "user@example.com"
        ))

        val notification = Notification(
            id = 3L,
            eventId = 125L,
            userId = 3L,
            channel = Channel.EMAIL,
            payload = payload,
            status = com.pitomets.notifications.domain.model.Status.NEW
        )

        whenever(mailSender.send(any<MimeMessage>())).doThrow(object : MailException("send fail") {})

        assertThrows(FailedToSendNotificationException::class.java) {
            sender.send(notification)
        }
    }
}
