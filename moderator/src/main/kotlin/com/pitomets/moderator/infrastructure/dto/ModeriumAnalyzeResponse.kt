package com.pitomets.moderator.infrastructure.dto

data class ModeriumAnalyzeResponse(
    val decision: ModeriumDecision? = null,
    val categories: ModeriumCategories? = null,
    val usage: ModeriumUsage? = null,
    val meta: ModeriumMeta? = null
)
