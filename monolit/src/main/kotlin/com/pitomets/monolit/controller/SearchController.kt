import com.pitomets.monolit.model.dto.request.SearchListingsRequest
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.service.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(
    private val searchService: SearchService
) {

    @GetMapping("/listings")
    fun searchListings(
        @RequestParam query: SearchListingsRequest
    ): List<SearchListingsResponse> {
        return searchService.search(query)
    }
}
