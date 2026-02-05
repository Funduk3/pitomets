package com.pitomets.monolit.model.dto.response

data class AnimalTypeDto(
    val id: Long,
    val title: String,
    val hasBreed: Boolean = false,
)
