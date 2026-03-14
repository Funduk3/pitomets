package com.pitomets.monolit.model.dto.response

data class PhotoModerationHintResponse(
    val status: String? = null,
    val reason: String? = null,
    val toxicityScore: Double? = null,
    val labels: List<String>? = null,
    val toxicTextDetected: Boolean? = null,
    val toxicTextMatches: List<String>? = null,
    val modelVersion: String? = null,
)
