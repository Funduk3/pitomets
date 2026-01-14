package com.pitomets.messenger.models

import kotlinx.serialization.Serializable

@Serializable
data class ReadReceiptEvent(
    val type: String,
    val chatId: Long,
    val readerId: Long,
)
