package com.pitomets.monolit.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexResponse
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.ConnectException

@Service
class SearchService(
    private val client: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(SearchService::class.java)

    @Suppress("TooGenericExceptionCaught")
    fun search(
        query: String,
        page: Int = 0,
        size: Int = 10,
    ): List<SearchListingsResponse> {
        return try {
            val from = page * size
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

            response.hits().hits()
                .mapNotNull { it.source() }
                .map { doc ->
                    SearchListingsResponse(
                        id = doc.id,
                        title = doc.title,
                        description = doc.description
                    )
                }
        } catch (e: ConnectException) {
            log.warn("Elasticsearch connection refused. Search unavailable. Query: {}", query, e)
            emptyList()
        } catch (e: Exception) {
            log.error("Error searching in Elasticsearch. Query: {}", query, e)
            emptyList()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun indexListing(doc: SearchListingDocument): IndexResponse? {
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
    }
}
