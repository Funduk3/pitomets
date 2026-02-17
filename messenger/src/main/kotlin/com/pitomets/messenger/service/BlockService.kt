package com.pitomets.messenger.service

import com.pitomets.messenger.models.UserBlock
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class BlockService {
    fun blockUser(blockerId: Long, blockedId: Long) {
        if (blockerId == blockedId) return
        transaction {
            val exists = UserBlock.select {
                (UserBlock.blockerId eq blockerId) and (UserBlock.blockedId eq blockedId)
            }.limit(1).empty().not()
            if (!exists) {
                UserBlock.insert {
                    it[UserBlock.blockerId] = blockerId
                    it[UserBlock.blockedId] = blockedId
                    it[UserBlock.createdAt] = Clock.System.now()
                }
            }
        }
    }

    fun unblockUser(blockerId: Long, blockedId: Long) {
        transaction {
            UserBlock.deleteWhere {
                (UserBlock.blockerId eq blockerId) and (UserBlock.blockedId eq blockedId)
            }
        }
    }

    fun isBlocked(blockerId: Long, blockedId: Long): Boolean {
        return transaction {
            UserBlock.select {
                (UserBlock.blockerId eq blockerId) and (UserBlock.blockedId eq blockedId)
            }.limit(1).empty().not()
        }
    }

    fun hasBlockBetween(userA: Long, userB: Long): Boolean {
        return isBlocked(userA, userB) || isBlocked(userB, userA)
    }
}
