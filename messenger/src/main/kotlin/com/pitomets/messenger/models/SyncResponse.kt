package com.pitomets.messenger.models

import com.pitomets.messenger.dto.MessageResponse
import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(
    val type: String,
    val messages: Map<String, List<MessageResponse>> // Map<chatId, messages>
)
