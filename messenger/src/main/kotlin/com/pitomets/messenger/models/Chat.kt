package com.pitomets.messenger.models

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Chat : LongIdTable("chats") {
    val user1Id = long("user1_id") // ID из монолита
    val user2Id = long("user2_id") // ID из монолита
    val listingId = long("listing_id").nullable()
    val listingTitle = varchar("listing_title", MAX_LISTING_TITLE_LENGTH).nullable()
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }

    // Unread counters per participant (unreadCount is per-user, not per-chat globally)
    val unreadCountUser1 = integer("unread_count_user1").default(0)
    val unreadCountUser2 = integer("unread_count_user2").default(0)
    val lastUnreadMessageIdUser1 = long("last_unread_message_id_user1").nullable()
    val lastUnreadMessageIdUser2 = long("last_unread_message_id_user2").nullable()

    const val MAX_LISTING_TITLE_LENGTH = 255
}
