package com.pitomets.messenger.db

import com.pitomets.messenger.models.Chats
import com.pitomets.messenger.models.Messages
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL")
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DATABASE_USER")
            password = System.getenv("DATABASE_PASSWORD")
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            // keep schema in sync (adds missing columns too)
            SchemaUtils.createMissingTablesAndColumns(Chats, Messages)
        }
    }
}
