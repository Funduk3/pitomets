package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.elastic.AutocompleteDoc
import com.pitomets.monolit.model.dto.response.SearchListingsPageResponse
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.service.SearchService
import com.pitomets.monolit.model.Gender
import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.SearchSort
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/search")
class SearchController(
    private val searchService: SearchService
) {
    private val mapper = jacksonObjectMapper()

    @GetMapping("/listings")
    fun searchListings(
        @RequestParam("query") query: String,
        @RequestParam("page", required = false, defaultValue = "0") page: Int = 0,
        @RequestParam("size", required = false, defaultValue = "10") size: Int = 10,
        @RequestParam("metro", required = false) metro: Long?,
        @RequestParam("city", required = false) city: Long?,
        @RequestParam("priceFrom", required = false) priceFrom: BigDecimal?,
        @RequestParam("priceTo", required = false) priceTo: BigDecimal?,
        @RequestParam("types", required = false) types: List<String>?,
        @RequestParam("breeds", required = false) breeds: List<String>?,
        @RequestParam("genders", required = false) genders: List<Gender>?,
        @RequestParam("ages", required = false) ages: List<AgeEnum>?,
        @RequestParam("sort", required = false, defaultValue = "NEWEST") sort: SearchSort = SearchSort.NEWEST,
        @RequestParam("searchAfter", required = false) searchAfter: String?,
    ): SearchListingsPageResponse =
        searchService.search(
            query,
            page,
            size,
            city,
            metro,
            priceFrom,
            priceTo,
            types,
            breeds,
            genders,
            ages,
            sort,
            searchAfter?.let { mapper.readValue<List<Any>>(it) },
        )

    @GetMapping("/listings/{id}/similar")
    fun moreLikeThis(
        @PathVariable("id") listingId: Long,
        @RequestParam("size", required = false, defaultValue = "10") size: Int = 10
    ): List<SearchListingsResponse> =
        searchService.moreLikeThis(listingId, size)

    @GetMapping("/listings/autocomplete")
    fun autocomplete(
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") size: Int
    ): List<AutocompleteDoc> =
        searchService.autocomplete(query, size)

    // delete it, only for dev
    @GetMapping("/deleteALL")
    fun deleteListings() =
        searchService.deleteIndex()
}
