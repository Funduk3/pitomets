package com.pitomets.moderator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moderium.api")
data class ModeriumApiProperties(
    val baseUrl: String = "https://moderium-ai.ru",
    val token: String = "",
    val mode: String = "strong",
    val withAnimal: Boolean = true
)
