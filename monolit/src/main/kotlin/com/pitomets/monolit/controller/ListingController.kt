package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.service.ListingsService
import com.pitomets.monolit.service.ReviewsService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/listings")
class ListingController(
    private val listingsService: ListingsService,
    private val reviewsService: ReviewsService,
) {
    @GetMapping("/")
    fun getListing(
        @RequestParam("id") listingId: Long
    ): ListingsResponse =
        listingsService.getListing(listingId)

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

    @DeleteMapping("/")
    fun deleteListing(
        @RequestParam("id") listingId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ) =
        listingsService.deleteListing(
            listingId,
            userPrincipal.id,
        )

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReview(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateReviewRequest
    ): ReviewResponse =
        reviewsService.createReview(userPrincipal.id, request)

    @GetMapping("/reviews")
    fun getListingReviews(
        @RequestParam("id") listingId: Long
    ): List<ReviewResponse> =
        reviewsService.getByListing(listingId)
}
