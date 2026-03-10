package com.pitomets.monolit.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PhotoUrlService(
    @Value("\${photos.public-base-url-images:/objects}") rawImagesBaseUrl: String,
    @Value("\${photos.public-base-url-avatars:/objects}") rawAvatarsBaseUrl: String
) {
    private val imagesBaseUrl = rawImagesBaseUrl.trimEnd('/')
    private val avatarsBaseUrl = rawAvatarsBaseUrl.trimEnd('/')

    fun objectUrl(objectKey: String): String {
        val normalizedKey = objectKey.trimStart('/')
        val base = if (normalizedKey.startsWith("avatars/")) avatarsBaseUrl else imagesBaseUrl
        return "$base/$normalizedKey"
    }
}
