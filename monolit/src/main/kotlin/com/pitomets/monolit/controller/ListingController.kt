package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.service.ListingsService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/listings")
class ListingController(
    private val listingsService: ListingsService
) {
    @GetMapping("/")
    fun getListing(
        @RequestParam("id") listingId: Long
    ): ListingsResponse {
        return listingsService.getListing(listingId)
    }

    @PostMapping("/")
    fun listings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: ListingsRequest
    ): ListingsResponse {
        return listingsService.createListing(userPrincipal.id, request)
    }

    @PutMapping("/")
    fun updateListing(
        @RequestParam("id") listingId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody updateListing: UpdateListingRequest,
    ): ListingsResponse {
        return listingsService.updateListing(
            listingId,
            userPrincipal.id,
            updateListing
        )
    }
}
