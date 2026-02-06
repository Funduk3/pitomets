package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.UpdateUserProfileRequest
import com.pitomets.monolit.model.dto.response.UserWithProfilesResponse
import com.pitomets.monolit.service.ProfileService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/profile")
class ProfileController(
    private val profileService: ProfileService
) {
    @GetMapping("/me")
    fun getCurrentUserProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): UserWithProfilesResponse =
        profileService.getUserWithProfiles(userPrincipal.id)

    @PutMapping("/me")
    fun updateCurrentUserProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: UpdateUserProfileRequest
    ): UserWithProfilesResponse =
        profileService.updateUserProfile(userPrincipal.id, request)

    // нужен, чтобы показывать имя в списках чатов (нужна авторизация)
    @GetMapping("/user/{userId}")
    fun getUserProfile(
        @PathVariable userId: Long
    ): UserWithProfilesResponse =
        profileService.getUserWithProfiles(userId)
}
