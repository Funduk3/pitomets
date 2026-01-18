package com.pitomets.monolit.model.dto.utils

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RegionDto(
    val id: Long,
    val idCountry: Long,
    val title: String
)
