package com.pitomets.monolit.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexResponse
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SearchService(
    private val client: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(SearchService::class.java)
    private val indexInitialized = AtomicBoolean(false)

    @Suppress("TooGenericExceptionCaught")
    private fun ensureIndexExists() {
        // Пытаемся создать индекс только один раз
        if (indexInitialized.get()) {
            return
        }

        synchronized(this) {
            // Двойная проверка
            if (!indexInitialized.get()) {
                try {
                    val existsResponse = client.indices().exists { e ->
                        e.index(INDEX)
                    }

                    if (!existsResponse.value()) {
                        log.info("Creating Elasticsearch index: {}", INDEX)
                        client.indices().create { c ->
                            c.index(INDEX)
                        }
                        log.info("Successfully created Elasticsearch index: {}", INDEX)
                    } else {
                        log.debug("Elasticsearch index '{}' already exists", INDEX)
                    }
                    indexInitialized.set(true)
                } catch (e: ConnectException) {
                    log.debug("Elasticsearch connection refused. Will retry later. Index: {}", INDEX)
                    // Не помечаем как инициализированный, чтобы попробовать снова позже
                } catch (e: Exception) {
                    log.error("Error creating Elasticsearch index: {}", INDEX, e)
                    // Не помечаем как инициализированный, чтобы попробовать снова позже
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun search(
        query: String,
        page: Int = DEFAULT_PAGE,
        size: Int = DEFAULT_SIZE,
    ): List<SearchListingsResponse> {
        ensureIndexExists()

        return try {
            val from = page * size
            log.debug("Searching Elasticsearch with query: '{}', page: {}, size: {}, from: {}", query, page, size, from)

            val response = client.search(
                { s ->
                    s.index(INDEX)
                        .from(from)
                        .size(size)
                        .query { q ->
                            q.multiMatch { mm ->
                                mm.query(query)
                                    .fields(listOf("title^3", "description"))
                            }
                        }
                },
                SearchListingDocument::class.java
            )

            val totalHits = response.hits().total()?.value() ?: 0
            log.info("Elasticsearch search returned {} hits for query: '{}'", totalHits, query)

            val results = response.hits().hits()
                .mapNotNull { it.source() }
                .map { doc ->
                    SearchListingsResponse(
                        id = doc.id,
                        title = doc.title,
                        description = doc.description
                    )
                }

            log.debug("Mapped {} results from Elasticsearch", results.size)
            results
        } catch (e: ConnectException) {
            log.warn("Elasticsearch connection refused. Search unavailable. Query: {}", query, e)
            emptyList()
        } catch (e: co.elastic.clients.elasticsearch._types.ElasticsearchException) {
            log.error(
                "Elasticsearch error during search. Query: '{}'. Status: {}, Message: {}",
                query,
                e.status(),
                e.message,
                e
            )
            emptyList()
        } catch (e: Exception) {
            log.error(
                "Error searching in Elasticsearch. Query: '{}'. Exception type: {}, Message: {}",
                query,
                e.javaClass.simpleName,
                e.message,
                e
            )
            emptyList()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun indexListing(doc: SearchListingDocument): IndexResponse? {
        ensureIndexExists()

        return try {
            client.index { idx ->
                idx.index(INDEX)
                    .id(doc.id.toString())
                    .document(doc)
            }
        } catch (e: ConnectException) {
            log.warn("Elasticsearch connection refused. Failed to index listing: {}", doc.id, e)
            null
        } catch (e: Exception) {
            log.error("Error indexing listing in Elasticsearch: {}", doc.id, e)
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun deleteListing(id: Long) {
        try {
            client.delete { d ->
                d.index(INDEX)
                    .id(id.toString())
            }
        } catch (e: ConnectException) {
            log.warn("Elasticsearch connection refused. Failed to delete listing: {}", id, e)
        } catch (e: Exception) {
            log.error("Error deleting listing from Elasticsearch: {}", id, e)
        }
    }
    companion object {
        private const val INDEX = "listings"
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SIZE = 10
    }
}
