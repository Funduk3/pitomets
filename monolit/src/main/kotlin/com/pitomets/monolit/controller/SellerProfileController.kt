package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.CreateSellerProfileRequest
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.service.ListingsService
import com.pitomets.monolit.service.ProfileService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/seller")
class SellerProfileController(
    private val profileService: ProfileService,
    private val listingsService: ListingsService,
) {

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSellerProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateSellerProfileRequest
    ): SellerProfileResponse {
        return profileService.createSellerProfile(userPrincipal.id, request)
    }

    @PutMapping("/profile")
    fun updateSellerProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateSellerProfileRequest
    ): SellerProfileResponse {
        return profileService.updateSellerProfile(userPrincipal.id, request)
    }

    @PostMapping("/listings")
    fun listings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: ListingsRequest
    ): ListingsResponse {
        return listingsService.createListing(userPrincipal.id, request)
    }
}
