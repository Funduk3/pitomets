package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.service.ListingReviewsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/review")
class ModeratorReviewsController(
    private val reviewsService: ListingReviewsService
) {
    @GetMapping("/all")
    fun getAllPendingReviews(): List<ReviewResponse> =
        reviewsService.getPendingReviews()


    @GetMapping("/{id}")
    fun getPendingReviewById(@PathVariable id: Long) =
        reviewsService.getPendingReview(id)

    @PostMapping("/{id}/approve")
    fun acceptReview(@PathVariable id: Long) {
        reviewsService.approveReview(id)
    }

    @PostMapping("/{id}/decline")
    fun declineReview(
        @PathVariable id: Long,
        @RequestBody adminMessage: AdminMessage
    ) {
        reviewsService.declineReview(id, adminMessage)
    }
}
