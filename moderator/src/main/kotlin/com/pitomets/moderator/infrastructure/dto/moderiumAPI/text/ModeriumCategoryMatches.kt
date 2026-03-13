package com.pitomets.moderator.infrastructure.dto.moderiumAPI.text

data class ModeriumCategoryMatches(
    val detected: Boolean? = null,
    val matches: List<String> = emptyList()
)