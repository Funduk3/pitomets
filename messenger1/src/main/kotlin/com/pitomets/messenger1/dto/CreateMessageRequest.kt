package com.pitomets.messenger1.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateMessageRequest(
    val chatId: Long,
    val content: String
)

