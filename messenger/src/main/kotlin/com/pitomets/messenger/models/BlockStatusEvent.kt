package com.pitomets.messenger.models

import kotlinx.serialization.Serializable

@Serializable
data class BlockStatusEvent(
    val type: String = "block_status",
    val otherUserId: Long,
    val blockedByMe: Boolean,
    val blockedMe: Boolean,
    val blockedAny: Boolean
)
