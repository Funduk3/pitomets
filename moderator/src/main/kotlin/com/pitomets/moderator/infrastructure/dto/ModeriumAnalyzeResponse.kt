package com.pitomets.moderator.infrastructure.dto

import com.pitomets.moderator.infrastructure.client.ModeriumCategories
import com.pitomets.moderator.infrastructure.client.ModeriumDecision

data class ModeriumAnalyzeResponse(
    val decision: ModeriumDecision? = null,
    val categories: ModeriumCategories? = null,
    val usage: ModeriumUsage? = null,
    val meta: ModeriumMeta? = null
)