package com.pitomets.notifications.interfaces.messaging

import com.pitomets.notifications.application.service.InvalidEventHandler
import com.pitomets.notifications.application.usecase.SendNotificationUseCase
import com.pitomets.notifications.interfaces.messaging.event.NotificationRequestedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KafkaNotificationConsumerTest {

    private val useCase: SendNotificationUseCase = mock()
    private val invalidEventHandler: InvalidEventHandler = mock()
    private val consumer = KafkaNotificationConsumer(useCase, invalidEventHandler)

    @Test
    fun `should execute use case for valid event`() {
        val event = validEvent()

        consumer.consume(event)

        verify(useCase, times(1)).execute(any())
        verify(invalidEventHandler, never()).handle(any(), any())
    }

    @Test
    fun `should route mapping error to invalid handler`() {
        val invalidEvent = validEvent(channel = "FAX")

        consumer.consume(invalidEvent)

        verify(useCase, never()).execute(any())
        verify(invalidEventHandler, times(1)).handle(any(), any())
    }

    @Test
    fun `should route illegal argument from use case to invalid handler`() {
        val event = validEvent(channel = "EMAIL")
        doThrow(IllegalArgumentException("No sender found")).whenever(useCase).execute(any())

        consumer.consume(event)

        verify(useCase, times(1)).execute(any())
        verify(invalidEventHandler, times(1)).handle(any(), any())
    }

    @Test
    fun `should not throw when invalid handler fails in unexpected exception branch`() {
        val event = validEvent(channel = "EMAIL")
        doThrow(RuntimeException("boom")).whenever(useCase).execute(any())
        doThrow(RuntimeException("handler down")).whenever(invalidEventHandler).handle(any(), any())

        assertDoesNotThrow {
            consumer.consume(event)
        }

        verify(useCase, times(1)).execute(any())
        verify(invalidEventHandler, times(1)).handle(any(), any())
    }

    private fun validEvent(channel: String = "EMAIL"): NotificationRequestedEvent =
        NotificationRequestedEvent(
            eventId = 10L,
            userId = 99L,
            channel = channel,
            messageType = "CONFIRM",
            payload = "user@example.com token"
        )
}
