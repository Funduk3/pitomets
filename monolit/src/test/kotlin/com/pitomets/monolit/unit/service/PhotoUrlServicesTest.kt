package com.pitomets.monolit.unit.service

import com.pitomets.monolit.service.PhotoModerationUrlService
import com.pitomets.monolit.service.PhotoUrlService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhotoUrlServicesTest {

    @Test
    fun `PhotoUrlService uses avatar base url for avatar keys`() {
        val service = PhotoUrlService(
            rawImagesBaseUrl = "https://cdn.example.com/images/",
            rawAvatarsBaseUrl = "https://cdn.example.com/avatars/"
        )

        val actual = service.objectUrl("avatars/user-1/avatar.jpg")

        assertEquals("https://cdn.example.com/avatars/avatars/user-1/avatar.jpg", actual)
    }

    @Test
    fun `PhotoUrlService uses image base url for non-avatar keys and trims slashes`() {
        val service = PhotoUrlService(
            rawImagesBaseUrl = "https://cdn.example.com/images///",
            rawAvatarsBaseUrl = "https://cdn.example.com/avatars///"
        )

        val actual = service.objectUrl("/listings/42/photo.png")

        assertEquals("https://cdn.example.com/images/listings/42/photo.png", actual)
    }

    @Test
    fun `PhotoModerationUrlService mirrors avatar routing behavior`() {
        val service = PhotoModerationUrlService(
            rawImagesBaseUrl = "https://moderation.example.com/img/",
            rawAvatarsBaseUrl = "https://moderation.example.com/ava/"
        )

        val actual = service.objectUrl("/avatars/u-7/a.webp")

        assertEquals("https://moderation.example.com/ava/avatars/u-7/a.webp", actual)
    }

    @Test
    fun `PhotoModerationUrlService returns image url for listing photo`() {
        val service = PhotoModerationUrlService(
            rawImagesBaseUrl = "https://moderation.example.com/img",
            rawAvatarsBaseUrl = "https://moderation.example.com/ava"
        )

        val actual = service.objectUrl("listing/10/p-1.jpg")

        assertEquals("https://moderation.example.com/img/listing/10/p-1.jpg", actual)
    }
}
