package com.pitomets.monolit.components

import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.dto.elastic.SearchListingDocument
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.service.SearchService
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ListingOutboxProcessor(
    private val outboxRepo: ListingOutboxRepository,
    private val searchService: SearchService
) {
    @Scheduled(fixedDelay = TIME_TO_ADD)
    @Transactional
    fun processOutbox() {
        val events = outboxRepo.findBatchForProcessing(BATCH_SIZE)

        if (events.isEmpty()) return

        events.forEach { event ->
            try {
                when (event.eventType) {
                    EventType.CREATE, EventType.UPDATE -> searchService.indexListing(
                        SearchListingDocument(
                            id = event.listingId,
                            title = event.title.orEmpty(),
                            description = event.description.orEmpty(),
                            city = event.city,
                            metro = event.metro,
                            price = event.price,
                        )
                    )
                    EventType.DELETE -> searchService.deleteListing(event.listingId)
                }
                outboxRepo.markProcessed(requireNotNull(event.id))
            } catch (_: Exception) {
                outboxRepo.incrementRetry(requireNotNull(event.id))
            }
        }

        log.info("Processed batch of ${events.size} events")
    }

    @Scheduled(fixedDelay = TIME_TO_DELETE)
    fun cleanupProcessedOutbox() {
        val ids = outboxRepo.findProcessedIds(BATCH_SIZE_TO_DELETE)
        if (ids.isEmpty()) {
            return
        }

        try {
            outboxRepo.deleteBatchByIds(ids)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val BATCH_SIZE = 50
        const val TIME_TO_ADD = 5000L
        const val TIME_TO_DELETE = 15000L
        const val BATCH_SIZE_TO_DELETE = 500
        private val log = LoggerFactory.getLogger(ListingOutboxProcessor::class.java)
    }
}
