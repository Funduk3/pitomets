package com.pitomets.messenger.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

object Messages : LongIdTable("messages") {
    val chatId = long("chat_id").references(Chats.id)
    val senderId = long("sender_id") // ID из монолита
    val content = text("content")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val isRead = bool("is_read").default(false)
}
