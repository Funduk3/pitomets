package com.pitomets.notifications.infrastructure.persistence.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.pitomets.notifications.domain.port.NotificationOutbox
import com.pitomets.notifications.infrastructure.outbox.OutboxEventEntity
import com.pitomets.notifications.infrastructure.outbox.OutboxJpaRepository
import org.springframework.stereotype.Component

@Component
class NotificationOutboxAdapter(
    private val outboxRepository: OutboxJpaRepository,
    private val objectMapper: ObjectMapper
) : NotificationOutbox {

    override fun save(event: Any) {
        val entity = OutboxEventEntity(
            eventType = event.javaClass.name,
            eventData = objectMapper.writeValueAsString(event),
            published = false
        )
        outboxRepository.save(entity)
    }
}
