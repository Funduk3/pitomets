package com.pitomets.moderator.infrastructure.dto.moderiumAPI.text

import com.pitomets.moderator.infrastructure.dto.moderiumAPI.text.ModeriumCategories
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumDecision
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumMeta
import com.pitomets.moderator.infrastructure.dto.moderiumAPI.ModeriumUsage

data class ModeriumAnalyzeResponse(
    val decision: ModeriumDecision? = null,
    val categories: ModeriumCategories? = null,
    val usage: ModeriumUsage? = null,
    val meta: ModeriumMeta? = null
)
