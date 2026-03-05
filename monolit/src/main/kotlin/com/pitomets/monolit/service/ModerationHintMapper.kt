package com.pitomets.monolit.service

import com.pitomets.monolit.model.dto.response.ModerationHintResponse

fun moderationHint(
    status: String?,
    reason: String?,
    toxicityScore: Double?,
    profanityDetected: Boolean?,
    sexualContentDetected: Boolean?,
    sourceAction: String?,
    modelVersion: String?
): ModerationHintResponse? {
    val hasData = !status.isNullOrBlank() ||
        !reason.isNullOrBlank() ||
        toxicityScore != null ||
        profanityDetected != null ||
        sexualContentDetected != null ||
        !sourceAction.isNullOrBlank() ||
        !modelVersion.isNullOrBlank()

    if (!hasData) {
        return null
    }

    return ModerationHintResponse(
        status = status,
        reason = reason,
        toxicityScore = toxicityScore,
        profanityDetected = profanityDetected,
        sexualContentDetected = sexualContentDetected,
        sourceAction = sourceAction,
        modelVersion = modelVersion
    )
}
