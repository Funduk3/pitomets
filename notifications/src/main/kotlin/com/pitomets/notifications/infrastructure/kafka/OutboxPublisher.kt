package com.pitomets.notifications.infrastructure.kafka

import com.pitomets.notifications.infrastructure.outbox.OutboxJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class OutboxPublisher(
    private val outboxRepository: OutboxJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(OutboxPublisher::class.java)

    companion object {
        private const val BATCH_SIZE = 50
        private const val TIMEOUT_SECONDS = 5L
    }

    @Scheduled(fixedDelay = 5000) // Каждые 5 секунд
    @Transactional
    fun publishEvents() {
        val events = outboxRepository.findUnpublishedBatch(BATCH_SIZE)

        if (events.isNotEmpty()) {
            logger.info("Found ${events.size} unpublished events")
        }

        events.forEach { event ->
            try {
                val key = event.id?.toString() ?: event.eventType
                val future = kafkaTemplate.send("notification.sent", key, event.eventData)
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                event.published = true
                outboxRepository.save(event)

                logger.debug("Published event: ${event.eventType} with id: ${event.id}")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.error("Interrupted while publishing event with id: ${event.id}", e)
                // не выбрасываем — переходим к следующему событию
            } catch (e: ExecutionException) {
                logger.error("Execution error while publishing event with id: ${event.id}", e)
                // оставить событие непомеченным для повторной попытки
            } catch (e: TimeoutException) {
                logger.error("Timeout while publishing event with id: ${event.id}", e)
                // оставить событие непомеченным для повторной попытки
            }
        }
    }
}
