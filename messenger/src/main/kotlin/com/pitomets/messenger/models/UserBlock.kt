package com.pitomets.messenger.models

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UserBlock : LongIdTable("user_blocks") {
    val blockerId = long("blocker_id")
    val blockedId = long("blocked_id")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }

    init {
        uniqueIndex("user_blocks_unique", blockerId, blockedId)
    }
}
