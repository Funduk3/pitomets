package com.pitomets.moderator.infrastructure.dto.moderiumAPI.photo

import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumDecision
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumMeta
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumUsage

data class ModeriumPhotoAnalyzeResponse(
    val decision: ModeriumDecision? = null,
    val categories: ModeriumPhotoCategories? = null,
    val usage: ModeriumUsage? = null,
    val meta: ModeriumMeta? = null
)