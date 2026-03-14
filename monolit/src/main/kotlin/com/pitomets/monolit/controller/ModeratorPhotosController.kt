package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.response.ModerationPhotoItemResponse
import com.pitomets.monolit.service.PhotoModerationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/photo")
class ModeratorPhotosController(
    private val photoModerationService: PhotoModerationService
) {
    @GetMapping("/all")
    fun getAllPendingPhotos(): List<ModerationPhotoItemResponse> =
        photoModerationService.getPendingPhotos()
}
