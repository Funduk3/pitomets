package src.main.kotlin.com.pitomets.messenger.models

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

data class User(val id: Int, val name: String)

object Users : IntIdTable() {
    val name = varchar("name", 50)
}