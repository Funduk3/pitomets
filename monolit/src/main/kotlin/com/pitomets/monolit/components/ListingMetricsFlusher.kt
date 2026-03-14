package com.pitomets.monolit.components

import com.pitomets.monolit.service.listing.ListingMetricsService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ListingMetricsFlusher(
    private val metricsService: ListingMetricsService
) {
    @Scheduled(fixedDelay = FLUSH_DELAY_MS)
    fun flush() {
        metricsService.flushDeltas()
    }

    companion object {
        private const val FLUSH_DELAY_MS = 60_000L
    }
}
