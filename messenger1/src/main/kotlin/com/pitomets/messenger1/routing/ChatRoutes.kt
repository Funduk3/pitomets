package com.pitomets.messenger1.routing

import com.pitomets.messenger1.dto.ChatResponse
import com.pitomets.messenger1.dto.CreateChatRequest
import com.pitomets.messenger1.service.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(chatService: ChatService) {
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
            call.respond(chats.map { ChatResponse.from(it) })
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

            call.respond(ChatResponse.from(chat))
        }
    }
}

