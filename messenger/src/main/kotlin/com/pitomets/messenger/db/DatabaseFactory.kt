package src.main.kotlin.com.pitomets.messenger.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/messenger",
            driver = "org.postgresql.Driver",
            user = "yourUsername",
            password = "yourPassword"
        )
    }

    fun getUserById(userId: Int): User? {
        return transaction {
            Users.select { Users.id eq userId }
                .map {
                    User(
                        id = it[Users.id],
                        name = it[Users.name]
                    )
                }
                .singleOrNull()
        }
    }
}