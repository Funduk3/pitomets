package com.pitomets.monolit.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PhotoUrlService(
    @Value("\${photos.public-base-url}") rawBaseUrl: String
) {
    private val baseUrl = rawBaseUrl.trimEnd('/')

    fun objectUrl(objectKey: String): String {
        val normalizedKey = objectKey.trimStart('/')
        return "$baseUrl/$normalizedKey"
    }
}
