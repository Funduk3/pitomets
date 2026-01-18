package com.pitomets.monolit.model.dto.response

data class MetroDto(
    val id: Long,
    val title: String,
    val line: MetroLineDto
)
