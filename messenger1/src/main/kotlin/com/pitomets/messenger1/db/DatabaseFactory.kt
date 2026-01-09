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
            // Поддержка как DATABASE_URL, так и отдельных переменных
            val dbUrl = System.getenv("DATABASE_URL")
            if (dbUrl != null) {
                jdbcUrl = dbUrl
            } else {
                val dbHost = System.getenv("DATABASE_HOST") ?: "localhost"
                val dbPort = System.getenv("DATABASE_PORT") ?: "5432"
                val dbName = System.getenv("DATABASE_NAME") ?: "messenger1"
                jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
            }
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

