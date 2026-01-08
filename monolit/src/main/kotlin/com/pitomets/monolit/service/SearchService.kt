package com.pitomets.monolit.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexResponse
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val client: ElasticsearchClient
) {
    fun search(
        query: String,
        page: Int = 0,
        size: Int = 10,
    ): List<SearchListingsResponse> {
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

        return response.hits().hits()
            .mapNotNull { it.source() }
            .map { doc ->
                SearchListingsResponse(
                    id = doc.id,
                    title = doc.title,
                    description = doc.description
                )
            }
    }

    fun indexListing(doc: SearchListingDocument): IndexResponse {
        return client.index { idx ->
            idx.index(INDEX)
                .id(doc.id.toString())
                .document(doc)
        }
    }

    fun deleteListing(id: Long) {
        client.delete { d ->
            d.index(INDEX)
                .id(id.toString())
        }
    }

    fun moreLikeThis(listingId: Long, size: Int = 10): List<SearchListingsResponse> {
        val response = client.search({ s ->
            s.index(INDEX)
                .size(size)
                .query { q ->
                    q.moreLikeThis { mlt ->
                        mlt.fields(listOf("title", "description"))
                        mlt.like { l ->
                            l.document { d ->
                                d.index(INDEX)
                                    .id(listingId.toString())
                            }
                        }
                        mlt.minTermFreq(1)
                        mlt.minDocFreq(1)
                    }
                }
        }, SearchListingDocument::class.java)
        return response.hits().hits()
            .mapNotNull { it.source() }
            .map { SearchListingsResponse(it.id, it.title, it.description) }
    }

    companion object {
        private const val INDEX = "listings"
    }
}
