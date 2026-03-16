package com.pitomets.moderator.infrastructure.client

import com.pitomets.moderator.config.ModeriumApiProperties
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.photo.ModeriumPhotoAnalyzeResponse
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.Base64

@Component
class HttpModeriumPhotoClient(
    private val properties: ModeriumApiProperties,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    fun analyze(photoURI: String): ModeriumPhotoAnalyzeResponse {
        require(properties.token.isNotBlank()) { "MODERIUM_API_TOKEN is empty" }

        if (shouldSendBase64(photoURI)) {
            val downloadUrl = normalizeDownloadUrl(photoURI)
            val bytes = restClient.get()
                .uri(downloadUrl)
                .retrieve()
                .body(ByteArray::class.java)
            check(bytes != null && bytes.isNotEmpty()) { "Moderium API photo download failed" }

            val base64 = Base64.getEncoder().encodeToString(bytes)
            val body = mapOf(
                "image_base64" to base64,
                "with_ocr" to true
            )

            val response = restClient.post()
                .uri("/api/v1/analyze-image")
                .header("X-API-Token", properties.token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ModeriumPhotoAnalyzeResponse::class.java)

            check(response != null) { "Moderium API returned empty body" }
            return response
        }

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

    private fun shouldSendBase64(photoURI: String): Boolean {
        return try {
            val host = URI(photoURI).host?.lowercase().orEmpty()
            host == "localhost" || host == "127.0.0.1" || host == "minio"
        } catch (_: Exception) {
            false
        }
    }

    private fun normalizeDownloadUrl(photoURI: String): String {
        return try {
            val uri = URI(photoURI)
            val host = uri.host?.lowercase()
            if (host == "localhost" || host == "127.0.0.1") {
                URI(
                    uri.scheme,
                    uri.userInfo,
                    "minio",
                    uri.port,
                    uri.path,
                    uri.query,
                    uri.fragment
                ).toString()
            } else {
                photoURI
            }
        } catch (_: Exception) {
            photoURI
        }
    }
}
