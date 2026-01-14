package com.pitomets.messenger.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateMessageRequest(
    val chatId: Long,
    val content: String
)
