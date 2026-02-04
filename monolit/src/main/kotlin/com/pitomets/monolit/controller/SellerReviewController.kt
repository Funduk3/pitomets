package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.service.SellerReviewsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
}
