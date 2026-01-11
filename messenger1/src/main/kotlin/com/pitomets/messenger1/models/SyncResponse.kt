package com.pitomets.messenger1.models

import com.pitomets.messenger1.dto.MessageResponse
import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(
    val type: String,
    val messages: Map<String, List<MessageResponse>> // Map<chatId, messages>
)