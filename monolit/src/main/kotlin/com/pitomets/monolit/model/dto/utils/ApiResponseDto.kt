package com.pitomets.monolit.model.dto.utils

data class ApiResponseDto<T>(
    val objects: List<T>,
    val total: Int,
    val more: Boolean
)
