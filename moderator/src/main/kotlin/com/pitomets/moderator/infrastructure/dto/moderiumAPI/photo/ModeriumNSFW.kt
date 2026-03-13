package com.pitomets.moderator.infrastructure.dto.moderiumAPI.photo

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumNSFW(
    @JsonProperty("score")
    val score: Double ?= null,
    @JsonProperty("detected")
    val detected: Boolean ?= null,
    @JsonProperty("labels")
    val labels: List<String> ?= null,
    @JsonProperty("available")
    val available: Boolean ?= null,
)
