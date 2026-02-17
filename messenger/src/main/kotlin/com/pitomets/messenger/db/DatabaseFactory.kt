package com.pitomets.messenger.db

import com.pitomets.messenger.models.Chat
import com.pitomets.messenger.models.Message
import com.pitomets.messenger.models.UserBlock
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val resolvedJdbcUrl = System.getenv("DATABASE_URL")
            ?: System.getenv("JDBC_DATABASE_URL")
            ?: "jdbc:postgresql://postgres-messenger:5432/messenger"

        val resolvedUser = System.getenv("DATABASE_USER") ?: System.getenv("JDBC_DATABASE_USER") ?: "user"
        val resolvedPassword = System.getenv("DATABASE_PASSWORD")
            ?: System.getenv("JDBC_DATABASE_PASSWORD") ?: "password"

        val config = HikariConfig().apply {
            jdbcUrl = resolvedJdbcUrl
            driverClassName = "org.postgresql.Driver"
            username = resolvedUser
            password = resolvedPassword
            maximumPoolSize = MAX_POOL_SIZE
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            // keep schema in sync (adds missing columns too)
            SchemaUtils.createMissingTablesAndColumns(Chat, Message, UserBlock)
        }
    }
    const val MAX_POOL_SIZE = 10
}
