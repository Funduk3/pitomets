package com.pitomets.monolit.config

import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.service.SearchService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * На старте приложения переиндексирует все объявления в Elasticsearch,
 * чтобы поиск работал даже после пересборки кластера/очистки индекса.
 */
@Component
class SearchReindexRunner(
    private val listingsRepo: ListingsRepo,
    private val searchService: SearchService
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(SearchReindexRunner::class.java)

    @Suppress("TooGenericExceptionCaught")
    override fun run(args: ApplicationArguments) {
        try {
            val listings = listingsRepo.findAll()
            if (listings.isEmpty()) {
                log.info("No listings found to reindex into Elasticsearch")
                return
            }

            var indexed = 0
            listings.forEach { listing ->
                val id = listing.id
                if (id == null) {
                    log.warn("Skip reindexing listing without id: {}", listing)
                    return@forEach
                }

                searchService.indexListing(
                    SearchListingDocument(
                        id = id,
                        title = listing.title,
                        description = listing.description
                    )
                )
                indexed++
            }

            log.info("Reindexed {} listings into Elasticsearch on startup", indexed)
        } catch (e: Exception) {
            log.warn("Failed to reindex listings into Elasticsearch on startup: {}", e.message, e)
        }
    }
}
