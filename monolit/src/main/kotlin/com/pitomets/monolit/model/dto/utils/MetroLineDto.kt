package com.pitomets.monolit.model.dto.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MetroLineDto(
    val id: Long,
    val title: String,
    val color: String,
    val stations: List<MetroStationDto> = emptyList()
)
