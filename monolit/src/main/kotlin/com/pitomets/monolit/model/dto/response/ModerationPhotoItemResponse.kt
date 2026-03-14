package com.pitomets.monolit.model.dto.response

data class ModerationPhotoItemResponse(
    val entityType: String,
    val entityId: Long,
    val photoId: Long? = null,
    val photoUrl: String,
    val photoUri: String,
    val moderationHint: PhotoModerationHintResponse? = null,
)
