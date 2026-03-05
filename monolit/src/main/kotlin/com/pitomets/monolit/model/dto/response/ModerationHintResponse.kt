package com.pitomets.monolit.model.dto.response

data class ModerationHintResponse(
    val status: String?,
    val reason: String?,
    val toxicityScore: Double?,
    val profanityDetected: Boolean?,
    val sexualContentDetected: Boolean?,
    val sourceAction: String?,
    val modelVersion: String?
)
