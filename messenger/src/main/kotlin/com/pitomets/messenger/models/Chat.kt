package com.pitomets.messenger.models

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Chats : LongIdTable("chats") {
    val user1Id = long("user1_id") // ID из монолита
    val user2Id = long("user2_id") // ID из монолита
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }

    // Unread counters per participant (unreadCount is per-user, not per-chat globally)
    val unreadCountUser1 = integer("unread_count_user1").default(0)
    val unreadCountUser2 = integer("unread_count_user2").default(0)
}
