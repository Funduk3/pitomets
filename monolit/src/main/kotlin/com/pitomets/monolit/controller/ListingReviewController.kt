package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.UpdateListingReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.service.ListingReviewsService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/listings/reviews")
class ListingReviewController(
    private val listingReviewsService: ListingReviewsService
) {
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    fun createListingReview(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateReviewRequest
    ): ReviewResponse =
        listingReviewsService.createListingReview(userPrincipal.id, request)

    @GetMapping("/")
    fun getListingReviews(
        @RequestParam("id") listingId: Long
    ): List<ReviewResponse> =
        listingReviewsService.getReviewByListing(listingId)

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.OK)
    fun deleteListingReview(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable reviewId: Long
    ) {
        listingReviewsService.deleteListingReview(
            currentUserId = userPrincipal.id,
            reviewId = reviewId
        )
    }

    @PutMapping("/")
    fun updateListingReview(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: UpdateListingReviewRequest
    ): ReviewResponse =
        listingReviewsService.updateListingReview(userPrincipal.id, request)
}
