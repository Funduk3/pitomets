package com.pitomets.monolit.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch.core.IndexResponse
import com.pitomets.monolit.model.SearchSort
import com.pitomets.monolit.model.dto.elastic.AutocompleteDoc
import com.pitomets.monolit.model.dto.elastic.SearchListingDocument
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.model.Gender
import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.dto.response.SearchListingsPageResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class SearchService(
    private val client: ElasticsearchClient
) {
    @Suppress("ExplicitItLambdaParameter", "LongMethod", "CyclomaticComplexMethod")
    fun search(
        query: String,
        page: Int = 0,
        size: Int = 10,
        cityId: Long? = null,
        metroId: Long? = null,
        priceFrom: BigDecimal? = null,
        priceTo: BigDecimal? = null,
        types: List<String>? = null,
        breeds: List<String>? = null,
        genders: List<Gender>? = null,
        ages: List<AgeEnum>? = null,
        sort: SearchSort = SearchSort.NEWEST,
        searchAfter: List<Any>? = null
    ): SearchListingsPageResponse {
        val from = page * size
        val response = client.search(
            { s ->
                s.index(INDEX)
                    .size(size)
                    .apply {
                        if (searchAfter.isNullOrEmpty()) {
                            from(from)
                        }
                    }
                    .sort { sortBuilder ->
                        when (sort) {
                            SearchSort.PRICE_ASC -> sortBuilder.field {
                                f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)
                            }
                            SearchSort.PRICE_DESC -> sortBuilder.field {
                                f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                            }
                            SearchSort.NEWEST -> sortBuilder.field {
                                f -> f.field("id").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                            }
                        }
                    }
                    .apply {
                        if (sort != SearchSort.NEWEST) {
                            sort { sortBuilder ->
                                sortBuilder.field {
                                    f -> f.field("id").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                }
                            }
                        }
                    }
                    .apply {
                        if (!searchAfter.isNullOrEmpty()) {
                            val values = searchAfter.mapNotNull { value ->
                                when (value) {
                                    is Int -> FieldValue.of(value.toLong())
                                    is Long -> FieldValue.of(value)
                                    is Double -> FieldValue.of(value)
                                    is Float -> FieldValue.of(value.toDouble())
                                    is Boolean -> FieldValue.of(value)
                                    is String -> FieldValue.of(value)
                                    else -> null
                                }
                            }
                            if (values.isNotEmpty()) {
                                searchAfter(values)
                            }
                        }
                    }
                    .query { q ->
                        q.bool { b ->
                            // search
                            b.must {
                                it.multiMatch { m ->
                                    m.query(query)
                                        .fields("title^3", "description")
                                        .fuzziness("AUTO")
                                        .prefixLength(MIN_CHAR_COUNT)
                                        .maxExpansions(MAX_EXPANSIONS_COUNT)
                                }
                            }
                            // filters
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
                            if (priceFrom != null || priceTo != null) {
                                b.filter { f ->
                                    f.range { rq ->
                                        rq.number { n ->
                                            n.field("price")
                                            priceFrom?.let { n.gte(it.toDouble()) }
                                            priceTo?.let { n.lte(it.toDouble()) }
                                            n
                                        }
                                        rq
                                    }
                                }
                            }
                            types?.takeIf { it.isNotEmpty() }?.let { values ->
                                b.filter { f ->
                                    f.terms { t ->
                                        t.field("species")
                                        t.terms { tv ->
                                            tv.value(values.map { v -> FieldValue.of(v) })
                                        }
                                    }
                                }
                            }
                            breeds?.takeIf { it.isNotEmpty() }?.let { values ->
                                b.filter { f ->
                                    f.terms { t ->
                                        t.field("breed")
                                        t.terms { tv ->
                                            tv.value(values.map { v -> FieldValue.of(v) })
                                        }
                                    }
                                }
                            }
                            genders?.takeIf { it.isNotEmpty() }?.let { values ->
                                val normalized = values
                                    .flatMap { g ->
                                        if (g == Gender.M || g == Gender.F) {
                                            listOf(g, Gender.ANY)
                                        } else {
                                            listOf(g)
                                        }
                                    }
                                    .distinct()
                                b.filter { f ->
                                    f.terms { t ->
                                        t.field("gender")
                                        t.terms { tv ->
                                            tv.value(normalized.map { v -> FieldValue.of(v.name) })
                                        }
                                    }
                                }
                            }
                            ages?.takeIf { it.isNotEmpty() }?.let { values ->
                                b.filter { f ->
                                    f.terms { t ->
                                        t.field("ageEnum")
                                        t.terms { tv ->
                                            tv.value(values.map { v -> FieldValue.of(v.name) })
                                        }
                                    }
                                }
                            }
                            // end of filters
                            b
                        }
                    }
            },
            SearchListingDocument::class.java
        )

        val hits = response.hits().hits()
        val items = hits
            .mapNotNull { it.source() }
            .map { doc ->
                SearchListingsResponse(
                    doc.id,
                    doc.title,
                    doc.description,
                    doc.price,
                    doc.cityTitle
                )
            }
        val nextSearchAfter = hits.lastOrNull()?.sort()?.mapNotNull { sv ->
            when {
                sv.isLong -> sv.longValue()
                sv.isDouble -> sv.doubleValue()
                sv.isBoolean -> sv.booleanValue()
                sv.isString -> sv.stringValue()
                else -> null
            }
        }

        return SearchListingsPageResponse(
            items = items,
            nextSearchAfter = nextSearchAfter
        )
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
            .map { SearchListingsResponse(it.id, it.title, it.description, it.price, it.cityTitle) }
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
        log.debug("DROP INDEX!!!")
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
