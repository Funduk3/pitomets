package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.request.SearchListingsRequest
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.service.SearchService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(
    private val searchService: SearchService
) {
    @PostMapping("/listings")
    fun searchListings(
        @RequestBody query: SearchListingsRequest
    ): List<SearchListingsResponse> =
        searchService.search(query)
}
