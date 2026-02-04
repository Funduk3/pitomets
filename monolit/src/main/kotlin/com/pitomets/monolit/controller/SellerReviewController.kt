package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateReviewRequest
import com.pitomets.monolit.model.dto.request.UpdateSellerReviewRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.service.SellerReviewsService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/seller/{sellerProfileId}/reviews")
class SellerReviewController(
    private val sellerReviewsService: SellerReviewsService
) {
    @GetMapping("/")
    fun getSellerReviews(
        @PathVariable sellerProfileId: Long
    ): List<ReviewResponse> =
        sellerReviewsService.getReviewsBySeller(sellerProfileId)

    @PostMapping("/")
    fun createSellerReview(
        @PathVariable sellerProfileId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateReviewRequest
    ): ReviewResponse =
        sellerReviewsService.createSellerReview(sellerProfileId, userPrincipal.id, request)

    @DeleteMapping("/{reviewId}")
    fun deleteSellerReview(
        @PathVariable sellerProfileId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable reviewId: Long,
    ) {
        sellerReviewsService.deleteSellerReview(sellerProfileId, userPrincipal.id, reviewId)
    }

    @PutMapping("/")
    fun updateSellerReview(
        @PathVariable sellerProfileId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: UpdateSellerReviewRequest
    ): ReviewResponse =
        sellerReviewsService.updateSellerReview(sellerProfileId, userPrincipal.id, request)
}
