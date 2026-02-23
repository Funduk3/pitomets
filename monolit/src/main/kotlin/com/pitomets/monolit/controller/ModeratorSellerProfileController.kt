package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.dto.response.SellerProfileResponse
import com.pitomets.monolit.service.ProfileService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/seller-profile")
class ModeratorSellerProfileController(
    private val profileService: ProfileService
) {
    @GetMapping("/all")
    fun getAllPendingSellerProfiles(): List<SellerProfileResponse> =
        profileService.getPendingSellerProfiles()

    @GetMapping("/{id}")
    fun getPendingSellerProfile(@PathVariable id: Long): SellerProfileResponse =
        profileService.getPendingSellerProfile(id)

    @PostMapping("/{id}/approve")
    fun approveSellerProfile(@PathVariable id: Long) {
        profileService.approveSellerProfile(id)
    }

    @PostMapping("/{id}/decline")
    fun declineSellerProfile(
        @PathVariable id: Long,
        @RequestBody adminMessage: AdminMessage
    ) {
        profileService.declineSellerProfile(id, adminMessage)
    }
}
