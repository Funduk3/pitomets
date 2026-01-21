package com.pitomets.monolit.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch.core.IndexResponse
import com.pitomets.monolit.model.dto.elastic.AutocompleteDoc
import com.pitomets.monolit.model.dto.elastic.SearchListingDocument
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val client: ElasticsearchClient
) {
    fun search(
        query: String,
        page: Int = 0,
        size: Int = 10,
        cityId: Long? = null,
        metroId: Long? = null
    ): List<SearchListingsResponse> {
        val from = page * size
        val response = client.search(
            { s ->
                s.index(INDEX)
                    .from(from)
                    .size(size)
                    .query { q ->
                        q.bool { b ->

                            b.must {
                                it.multiMatch { m ->
                                    m.query(query)
                                        .fields("title^3", "description")
                                        .fuzziness("AUTO")
                                        .prefixLength(MIN_CHAR_COUNT)
                                        .maxExpansions(MAX_EXPANSIONS_COUNT)
                                }
                            }

                            cityId?.let { city ->
                                b.filter {
                                    it.term { t ->
                                        t.field("city").value(city)
                                    }
                                }
                            }
                            metroId?.let { metro ->
                                b.filter {
                                    it.term { t ->
                                        t.field("metro").value(metro)
                                    }
                                }
                            }

                            b
                        }
                    }
            },
            SearchListingDocument::class.java
        )

        return response.hits().hits()
            .mapNotNull { it.source() }
            .map { doc ->
                SearchListingsResponse(
                    doc.id,
                    doc.title,
                    doc.description
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

    fun autocomplete(query: String, size: Int): List<AutocompleteDoc> {
        if (query.length < MIN_CHAR_NUMBER ||
            query.split(" ").size > MAX_WORD_COUNT ||
            query.isBlank()
        ) {
            return emptyList()
        }

        val response = client.search(
            { s ->
                s.index(INDEX)
                    .size(size)
                    .source { src -> src.filter { f -> f.includes("title") } }
                    .query { q ->
                        q.multiMatch { mm ->
                            mm.query(query)
                                .type(TextQueryType.BoolPrefix)
                                .fields(
                                    "title",
                                    "title._2gram",
                                    "title._3gram"
                                )
                        }
                    }
            },
            AutocompleteDoc::class.java
        )

        return response.hits().hits()
            .mapNotNull { it.source() }
    }

    // only for dev
    fun deleteIndex() {
        client.indices().delete { d ->
            d.index(INDEX)
        }
        log.info("DROP INDEX!!!")
    }

    companion object {
        private const val INDEX = "listings"
        private const val MIN_CHAR_NUMBER = 3
        private const val MAX_WORD_COUNT = 5
        private const val MIN_CHAR_COUNT = 1
        private const val MAX_EXPANSIONS_COUNT = 25
        private val log = LoggerFactory.getLogger(SearchService::class.java)
    }
}
