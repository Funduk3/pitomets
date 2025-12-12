package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.service.ReviewsService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/reviews")
class ReviewController(
    private val reviewsService: ReviewsService
) {
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReview(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateReviewRequest
    ): ReviewResponse =
        reviewsService.createReview(userPrincipal.id, request)

    @GetMapping("/listing")
    fun getListingReviews(
        @RequestParam("id") listingId: Long
    ): List<ReviewResponse> =
        reviewsService.getByListing(listingId)

    @GetMapping("/seller")
    fun getSellerReviews(
        @RequestParam("id") sellerProfileId: Long
    ): List<ReviewResponse> =
        reviewsService.getBySeller(sellerProfileId)
}
