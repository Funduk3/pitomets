package com.pitomets.moderator.infrastructure.dto.moderiumAPI.photo

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumOCR(
    @JsonProperty("enabled")
    val enabled: Boolean ?= null,
    @JsonProperty("detected_text")
    val detectedText: Boolean ?= null,
    @JsonProperty("preview")
    val preview: List<String> ?= null,
    @JsonProperty("toxic_text_detected")
    val toxicTextDetected: Boolean ?= null,
    @JsonProperty("toxic_matches")
    val toxicMatches: List<String> ?= null,
    @JsonProperty("available")
    val available: Boolean ?= null,
)
