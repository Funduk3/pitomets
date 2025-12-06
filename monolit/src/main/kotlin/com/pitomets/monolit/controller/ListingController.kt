package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.service.ListingsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/listing")
class ListingController(
    private val listingsService: ListingsService
) {
    @GetMapping("/")
    fun getListing(
        @RequestParam("id")
        listingId: Long
    ): ListingsResponse {
        return listingsService.getListing(listingId)
    }
}
