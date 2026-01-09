package com.pitomets.messenger1.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.pitomets.messenger1.models.Chats
import com.pitomets.messenger1.models.Messages
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            // Аналогично monolit - используем полный DATABASE_URL
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/messenger1"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DATABASE_USER") ?: "user"
            password = System.getenv("DATABASE_PASSWORD") ?: "password"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(Chats, Messages)
        }
    }
}

