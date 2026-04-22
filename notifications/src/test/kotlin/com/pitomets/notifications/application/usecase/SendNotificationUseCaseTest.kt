package com.pitomets.notifications.application.usecase

import com.pitomets.notifications.application.command.SendNotificationCommand
import com.pitomets.notifications.application.event.NotificationFailedEvent
import com.pitomets.notifications.application.event.NotificationSentEvent
import com.pitomets.notifications.domain.model.Channel
import com.pitomets.notifications.domain.model.MessageType
import com.pitomets.notifications.domain.model.Notification
import com.pitomets.notifications.domain.model.Status
import com.pitomets.notifications.domain.port.NotificationOutbox
import com.pitomets.notifications.domain.port.NotificationRepository
import com.pitomets.notifications.domain.port.NotificationSender
import com.pitomets.notifications.exceptions.FailedToSendNotificationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

class SendNotificationUseCaseTest {

    private val notificationRepository: NotificationRepository = mock()
    private val notificationOutbox: NotificationOutbox = mock()
    private val emailSender: NotificationSender = mock()
    private val smsSender: NotificationSender = mock()

    private val useCase = SendNotificationUseCase(
        notificationRepository = notificationRepository,
        notificationOutbox = notificationOutbox,
        notificationSenders = listOf(emailSender, smsSender)
    )

    @Test
    fun `should skip processing when event already exists`() {
        val command = testCommand()
        whenever(notificationRepository.existsByEventId(command.eventId)).thenReturn(true)

        assertDoesNotThrow { useCase.execute(command) }

        verify(notificationRepository, never()).save(any())
        verify(emailSender, never()).send(any())
        verify(notificationOutbox, never()).save(any())
    }

    @Test
    fun `should save sent status and outbox event when sending succeeds`() {
        val command = testCommand(channel = Channel.EMAIL)
        val saved = Notification.create(command).copy(id = 101L)

        whenever(notificationRepository.existsByEventId(command.eventId)).thenReturn(false)
        whenever(notificationRepository.save(any())).thenReturn(saved, saved.markSent())
        whenever(emailSender.channel()).thenReturn(Channel.EMAIL)
        whenever(smsSender.channel()).thenReturn(Channel.SMS)

        useCase.execute(command)

        verify(emailSender, times(1)).send(saved)
        val notificationCaptor = argumentCaptor<Notification>()
        verify(notificationRepository, times(2)).save(notificationCaptor.capture())
        assertEquals(Status.NEW, notificationCaptor.firstValue.status)
        assertEquals(Status.SENT, notificationCaptor.secondValue.status)

        val outboxCaptor = argumentCaptor<Any>()
        verify(notificationOutbox, times(1)).save(outboxCaptor.capture())
        val sentEvent = outboxCaptor.firstValue as NotificationSentEvent
        assertEquals(command.eventId, sentEvent.eventId)
        assertEquals(command.userId, sentEvent.userId)
        assertEquals(command.channel, sentEvent.channel)
        assertEquals(saved.id, sentEvent.notificationId)
    }

    @Test
    fun `should mark failed and write failed event when sender throws FailedToSendNotificationException`() {
        val command = testCommand(channel = Channel.EMAIL)
        val saved = Notification.create(command).copy(id = 202L)

        whenever(notificationRepository.existsByEventId(command.eventId)).thenReturn(false)
        whenever(notificationRepository.save(any())).thenReturn(saved, saved.markFailed())
        whenever(emailSender.channel()).thenReturn(Channel.EMAIL)
        whenever(smsSender.channel()).thenReturn(Channel.SMS)
        doThrow(FailedToSendNotificationException("SMTP down", null)).whenever(emailSender).send(saved)

        assertDoesNotThrow { useCase.execute(command) }

        val notificationCaptor = argumentCaptor<Notification>()
        verify(notificationRepository, times(2)).save(notificationCaptor.capture())
        assertEquals(Status.NEW, notificationCaptor.firstValue.status)
        assertEquals(Status.FAILED, notificationCaptor.secondValue.status)

        val outboxCaptor = argumentCaptor<Any>()
        verify(notificationOutbox).save(outboxCaptor.capture())
        val failedEvent = outboxCaptor.firstValue as NotificationFailedEvent
        assertEquals("SMTP down", failedEvent.error)
        assertEquals(command.eventId, failedEvent.eventId)
        assertEquals(saved.id, failedEvent.notificationId)
    }

    @Test
    fun `should return when unique constraint race happens on first save`() {
        val command = testCommand(channel = Channel.EMAIL)
        whenever(notificationRepository.existsByEventId(command.eventId)).thenReturn(false)
        whenever(emailSender.channel()).thenReturn(Channel.EMAIL)
        whenever(smsSender.channel()).thenReturn(Channel.SMS)
        whenever(notificationRepository.save(any())).thenThrow(DataIntegrityViolationException("duplicate"))

        assertDoesNotThrow { useCase.execute(command) }

        verify(emailSender, never()).send(any())
        verify(notificationOutbox, never()).save(any())
    }

    @Test
    fun `should throw when sender for channel is missing`() {
        val command = testCommand(channel = Channel.PUSH)
        val saved = Notification.create(command).copy(id = 303L)

        whenever(notificationRepository.existsByEventId(command.eventId)).thenReturn(false)
        whenever(notificationRepository.save(any())).thenReturn(saved)
        whenever(emailSender.channel()).thenReturn(Channel.EMAIL)
        whenever(smsSender.channel()).thenReturn(Channel.SMS)

        val exception = assertThrows<IllegalArgumentException> { useCase.execute(command) }
        assertEquals("No sender found for channel: ${command.channel}", exception.message)
        verify(notificationOutbox, never()).save(any())
    }

    private fun testCommand(channel: Channel = Channel.EMAIL) = SendNotificationCommand(
        eventId = 11L,
        userId = 22L,
        channel = channel,
        messageType = MessageType.CONFIRM,
        payload = "user@example.com token-123"
    )
}
