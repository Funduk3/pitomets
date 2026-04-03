package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsCursorResponse
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.service.listing.ListingsService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val listingsService: ListingsService,
) {
    @GetMapping("/")
    fun getListing(
        @RequestParam("id") listingId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?,
        request: HttpServletRequest,
    ): ListingsResponse =
        listingsService.getListingWithView(
            listingId = listingId,
            viewerId = userPrincipal?.id,
            viewerIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                ?: request.remoteAddr,
            userAgent = request.getHeader("User-Agent")
        )

    @GetMapping("/my")
    fun getMyListings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): List<ListingsResponse> =
        listingsService.getUserListings(userPrincipal.id)

    @GetMapping("/seller")
    fun getSellerListings(
        @RequestParam("sellerId") sellerId: Long,
        @RequestParam("archived", required = false, defaultValue = "false") archived: Boolean
    ): List<ListingsResponse> =
        listingsService.getSellerListingsPublic(sellerId, archived)

    @GetMapping("/home")
    fun getHomeListings(
        @RequestParam("cursor", required = false) cursor: Long?
    ): ListingsCursorResponse =
        listingsService.getHomeListings(cursor)

    @PostMapping("/")
    fun createListings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: ListingsRequest
    ): ListingsResponse =
        listingsService.createListing(
            userPrincipal.id,
            request
        )

    @PutMapping("/")
    fun updateListing(
        @RequestParam("id") listingId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody updateListing: UpdateListingRequest,
    ): ListingsResponse =
        listingsService.updateListing(
            listingId,
            userPrincipal.id,
            updateListing
        )

    @PutMapping("/archive")
    fun archiveListing(
        @RequestParam("id") listingId: Long,
        @RequestParam("archived", required = false, defaultValue = "true") archived: Boolean,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): ListingsResponse =
        listingsService.setListingArchived(
            listingId,
            userPrincipal.id,
            archived
        )

    @DeleteMapping("/")
    fun deleteListing(
        @RequestParam("id") listingId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ) =
        listingsService.deleteListing(
            listingId,
            userPrincipal.id,
        )
}
