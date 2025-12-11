package com.pitomets.monolit.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.request.SearchListingsRequest
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val client: ElasticsearchClient
) {
    fun search(
        query: SearchListingsRequest
    ): List<SearchListingsResponse> {
        val from = query.page * query.size

        val response = client.search(
            { s ->
                s.index("listings")
                    .from(from)
                    .size(query.size)
                    .query { q ->
                        q.multiMatch { mm ->
                            mm.query(query.query)
                                .fields(listOf("title^3", "description"))
                        }
                    }
            },
            Map::class.java
        )

        val mapper = jacksonObjectMapper().registerKotlinModule() // todo поправить

        return response.hits().hits()
            .mapNotNull { hit ->
                val source = hit.source() ?: return@mapNotNull null
                val doc = mapper.convertValue(source, SearchListingDocument::class.java)
                SearchListingsResponse(
                    id = doc.id,
                    title = doc.title,
                    description = doc.description
                )
            }
    }

    fun indexListing(doc: SearchListingDocument): IndexResponse {
        return client.index { idx ->
            idx.index("listings")
                .id(doc.id.toString())
                .document(doc)
                // Это плохо, потом поправить
                .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
        }
    }

    fun deleteListing(id: Long) {
        client.delete { d ->
            d.index("listings")
                .id(id.toString())
                // Это плохо, потом поправить
                .refresh(co.elastic.clients.elasticsearch._types.Refresh.True)
        }
    }
}
