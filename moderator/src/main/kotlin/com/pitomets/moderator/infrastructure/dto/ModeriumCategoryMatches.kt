package com.pitomets.moderator.infrastructure.dto

data class ModeriumCategoryMatches(
    val detected: Boolean? = null,
    val matches: List<String> = emptyList()
)
