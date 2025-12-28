package com.pitomets.monolit.model.dto.response

data class UploadPhotoResponse(
    val photoId: Long,
    val objectKey: String,
    val position: Int
)
