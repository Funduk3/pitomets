package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.response.AvatarUrlResponse
import com.pitomets.monolit.model.dto.response.DeleteAvatarResponse
import com.pitomets.monolit.model.dto.response.UploadAvatarResponse
import com.pitomets.monolit.service.UserPhotoService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/users/photos")
class UserPhotoController(
    private val userPhotoService: UserPhotoService
) {

    @PostMapping("/avatar")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadAvatar(
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal user: UserPrincipal
    ): UploadAvatarResponse {
        val key = userPhotoService.uploadAvatar(file, user.id)
        return UploadAvatarResponse(avatarKey = key)
    }

    @GetMapping("/avatar")
    fun getAvatar(
        @AuthenticationPrincipal user: UserPrincipal
    ): AvatarUrlResponse {
        val url = userPhotoService.getAvatarUrlOrNull(user.id, includeUnapproved = true)
        return AvatarUrlResponse(url = url)
    }

    @GetMapping("/avatar/{userId}")
    fun getAvatarByUserId(
        @PathVariable userId: Long
    ): AvatarUrlResponse {
        val url = userPhotoService.getAvatarUrlOrNull(userId, includeUnapproved = false)
        return AvatarUrlResponse(url = url)
    }

    @DeleteMapping("/avatar")
    fun deleteAvatar(
        @AuthenticationPrincipal user: UserPrincipal
    ): DeleteAvatarResponse {
        val key = userPhotoService.deleteAvatar(user.id)
        return DeleteAvatarResponse(
            avatarKey = key
        )
    }
}
