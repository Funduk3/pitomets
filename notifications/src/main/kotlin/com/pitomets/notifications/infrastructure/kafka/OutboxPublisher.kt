package com.pitomets.notifications.infrastructure.kafka

import com.pitomets.notifications.exceptions.PublishEventException
import com.pitomets.notifications.infrastructure.outbox.OutboxJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxPublisher(
    private val outboxRepository: OutboxJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(OutboxPublisher::class.java)

    @Scheduled(fixedDelay = 5000) // Каждые 5 секунд
    @Transactional
    fun publishEvents() {
        val events = outboxRepository.findUnpublished()

        if (events.isNotEmpty()) {
            logger.info("Found ${events.size} unpublished events")
        }

        events.forEach { event ->
            try {
                kafkaTemplate.send("notification-events", event.eventData)

                event.published = true
                outboxRepository.save(event)

                logger.debug("Published event: ${event.eventType} with id: ${event.id}")
            } catch (e: PublishEventException) {
                logger.error("Failed to publish event with id: ${event.id}", e)
            }
        }
    }
}
