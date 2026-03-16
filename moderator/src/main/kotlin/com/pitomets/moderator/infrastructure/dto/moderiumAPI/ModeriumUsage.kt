package com.pitomets.moderator.infrastructure.dto.moderiumAPI

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumUsage(
    @JsonProperty("request_tokens")
    val requestTokens: Int? = null,
    @JsonProperty("remaining_tokens")
    val remainingTokens: Int? = null
)
