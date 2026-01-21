package com.pitomets.monolit.model.dto.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetroStationDto(
    val id: Long,
    val title: String
)
