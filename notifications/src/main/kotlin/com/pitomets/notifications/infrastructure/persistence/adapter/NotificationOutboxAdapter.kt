package com.pitomets.notifications.infrastructure.persistence.adapter

import com.pitomets.notifications.domain.port.NotificationOutbox
import com.pitomets.notifications.infrastructure.outbox.OutboxEventEntity
import com.pitomets.notifications.infrastructure.outbox.OutboxJpaRepository
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class NotificationOutboxAdapter(
    private val outboxRepository: OutboxJpaRepository,
    private val objectMapper: ObjectMapper
) : NotificationOutbox {

    override fun save(event: Any) {
        val entity = OutboxEventEntity(
            eventType = event.javaClass.simpleName,
            eventData = objectMapper.writeValueAsString(event),
            published = false
        )
        outboxRepository.save(entity)
    }
}