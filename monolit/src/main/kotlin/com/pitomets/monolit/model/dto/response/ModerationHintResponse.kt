package com.pitomets.monolit.model.dto.response

data class ModerationHintResponse(
    val status: String?,
    val reason: String?,
    val toxicityScore: Double?,
    val sourceAction: String?,
    val modelVersion: String?
)
