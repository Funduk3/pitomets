import co.elastic.clients.elasticsearch.ElasticsearchClient
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
        val response = client.search(
            { s ->
                s.index("listings")
                    .query { q ->
                        q.multiMatch { mm ->
                            mm.query(query.query)
                                .fields(listOf("tittle^3", "description")) // не доделал
                        }
                    }
            },
            SearchListingDocument::class.java
        )

        return response.hits().hits()
            .mapNotNull { hit ->
                hit.source()?.let { doc ->
                    SearchListingsResponse(
                        id = doc.id,
                        title = doc.title,
                        description = doc.description
                    )
                }
            }
    }
}
