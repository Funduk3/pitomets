package com.pitomets.messenger.routing

import com.pitomets.messenger.dto.ChatResponse
import com.pitomets.messenger.dto.CreateChatRequest
import com.pitomets.messenger.service.ChatService
import com.pitomets.messenger.service.MessageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(chatService: ChatService, messageService: MessageService) {
    route("/api/chats") {
        // Создать или получить чат
        post {
            val request = call.receive<CreateChatRequest>()
            val currentUserId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            val chat = chatService.createOrGetChat(currentUserId, request.userId)
            call.respond(HttpStatusCode.OK, ChatResponse.from(chat))
        }

        // Получить все чаты пользователя
        get {
            val userId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            val chats = chatService.getUserChats(userId)
            val result = chats.map { chat ->
                val last = messageService.getLastMessageByChatId(chat.id)
                val unreadCount = if (userId == chat.user1Id) chat.unreadCountUser1 else chat.unreadCountUser2
                ChatResponse.from(
                    chat,
                    unreadCount = unreadCount,
                    lastMessage = last?.let { com.pitomets.messenger.dto.MessageResponse.from(it) }
                )
            }
            call.respond(result)
        }

        // Получить чат по ID
        get("/{chatId}") {
            val chatId = call.parameters["chatId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid chat ID")

            val userId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            val chat = chatService.getChatById(chatId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Chat not found")

            if (!chatService.isUserInChat(chatId, userId)) {
                return@get call.respond(HttpStatusCode.Forbidden, "User is not in this chat")
            }

            val last = messageService.getLastMessageByChatId(chat.id)
            val unreadCount = if (userId == chat.user1Id) chat.unreadCountUser1 else chat.unreadCountUser2
            call.respond(
                ChatResponse.from(
                    chat,
                    unreadCount = unreadCount,
                    lastMessage = last?.let { com.pitomets.messenger.dto.MessageResponse.from(it) }
                )
            )
        }
    }
}

