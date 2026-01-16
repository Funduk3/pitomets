package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.response.ReviewResponse
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.service.ProfileService
import com.pitomets.monolit.service.ReviewsService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/seller")
class SellerProfileController(
    private val profileService: ProfileService,
    private val reviewsService: ReviewsService,
) {
    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSellerProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateSellerProfileRequest
    ): SellerProfileResponse =
        profileService.createSellerProfile(userPrincipal.id, request)

    @PutMapping("/profile")
    fun updateSellerProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateSellerProfileRequest
    ): SellerProfileResponse =
        profileService.updateSellerProfile(userPrincipal.id, request)

    @GetMapping("/reviews")
    fun getSellerReviews(
        @RequestParam("id") sellerProfileId: Long
    ): List<ReviewResponse> =
        reviewsService.getBySeller(sellerProfileId)

    @GetMapping("/profile")
    fun getSellerProfile(
        @RequestParam("sellerId") sellerId: Long
    ): SellerProfileResponse =
        profileService.getSellerProfileByUserId(sellerId)
}
