package com.pitomets.moderator.infrastructure.client

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumAnalyzeRequest(
    val text: String,
    val mode: String,
    @JsonProperty("with_animal")
    val withAnimal: Boolean
)

data class ModeriumAnalyzeResponse(
    val decision: ModeriumDecision? = null,
    val categories: ModeriumCategories? = null,
    val usage: ModeriumUsage? = null,
    val meta: ModeriumMeta? = null
)

data class ModeriumDecision(
    val action: String? = null,
    val mode: String? = null,
    val reason: String? = null
)

data class ModeriumCategories(
    val profanity: ModeriumCategoryMatches? = null,
    @JsonProperty("sexual_content")
    val sexualContent: ModeriumCategoryMatches? = null,
    val toxicity: ModeriumToxicity? = null
)

data class ModeriumCategoryMatches(
    val detected: Boolean? = null,
    val matches: List<String> = emptyList()
)

data class ModeriumToxicity(
    val score: Double? = null
)

data class ModeriumUsage(
    @JsonProperty("request_tokens")
    val requestTokens: Int? = null,
    @JsonProperty("remaining_tokens")
    val remainingTokens: Int? = null
)

data class ModeriumMeta(
    @JsonProperty("request_id")
    val requestId: String? = null,
    @JsonProperty("processing_time_ms")
    val processingTimeMs: Long? = null,
    @JsonProperty("model_version")
    val modelVersion: String? = null
)
