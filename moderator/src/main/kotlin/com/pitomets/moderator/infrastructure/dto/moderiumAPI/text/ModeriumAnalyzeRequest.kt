package com.pitomets.moderator.infrastructure.dto.moderiumAPI.text

import com.fasterxml.jackson.annotation.JsonProperty

data class ModeriumAnalyzeRequest(
    val text: String,
    val mode: String,
    @JsonProperty("with_animal")
    val withAnimal: Boolean
)
