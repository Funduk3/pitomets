package com.pitomets.monolit.model.dto.utils

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CityDto(
    val id: Long,
    val idRegion: Long,
    val idCountry: Long,
    val title: String,
    val titleEng: String,
    val nameGenitive: String? = null,
    val nameDeclension: String? = null
)
