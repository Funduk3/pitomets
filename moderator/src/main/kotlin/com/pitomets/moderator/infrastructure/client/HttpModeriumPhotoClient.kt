package com.pitomets.moderator.infrastructure.client

import com.pitomets.moderator.config.ModeriumApiProperties
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.photo.ModeriumPhotoAnalyzeResponse
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class HttpModeriumPhotoClient(
    private val properties: ModeriumApiProperties,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    fun analyze(photoURI: String): ModeriumPhotoAnalyzeResponse {
        require(properties.token.isNotBlank()) { "MODERIUM_API_TOKEN is empty" }

        val photoBodyBuilder = MultipartBodyBuilder()
        photoBodyBuilder.part("image_url", photoURI)
        photoBodyBuilder.part("with_ocr", "true")

        val response = restClient.post()
            .uri("/api/v1/analyze-image")
            .header("X-API-Token", properties.token)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(photoBodyBuilder.build())
            .retrieve()
            .body(ModeriumPhotoAnalyzeResponse::class.java)

        check(response != null) { "Moderium API returned empty body" }

        return response
    }
}