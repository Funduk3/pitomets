package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.response.UserWithProfilesResponse
import com.pitomets.monolit.service.ProfileService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    /**
     * Публичный для авторизованных: нужен, чтобы показывать имя (человек/магазин) в списках чатов.
     */
    @GetMapping("/user/{userId}")
    fun getUserProfile(
        @PathVariable userId: Long
    ): UserWithProfilesResponse =
        profileService.getUserWithProfiles(userId)
}
