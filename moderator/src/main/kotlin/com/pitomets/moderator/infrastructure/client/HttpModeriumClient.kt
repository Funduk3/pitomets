package com.pitomets.moderator.infrastructure.client

import com.pitomets.moderator.config.ModeriumApiProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class HttpModeriumClient(
    private val properties: ModeriumApiProperties
) : ModeriumClient {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    override fun analyze(
        text: String,
        mode: String,
        withAnimal: Boolean
    ): ModeriumAnalyzeResponse {
        require(properties.token.isNotBlank()) { "MODERIUM_API_TOKEN is empty" }

        val request = ModeriumAnalyzeRequest(
            text = text,
            mode = mode,
            withAnimal = withAnimal
        )

        return restClient.post()
            .uri("/api/v1/analyze")
            .header("X-API-Token", properties.token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ModeriumAnalyzeResponse::class.java)
            ?: throw IllegalStateException("Moderium API returned empty body")
    }
}
