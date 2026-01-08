package src.main.kotlin.com.pitomets.messenger.models

object Messages : IntIdTable() {
    val userId = integer("user_id").references(Users.id)
    val content = varchar("content", 255)
    val timestamp = long("timestamp")
}