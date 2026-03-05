package com.pitomets.moderator.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumMeta(
    @JsonProperty("request_id")
    val requestId: String? = null,
    @JsonProperty("processing_time_ms")
    val processingTimeMs: Long? = null,
    @JsonProperty("model_version")
    val modelVersion: String? = null
)