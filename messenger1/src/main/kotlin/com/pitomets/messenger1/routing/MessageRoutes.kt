package com.pitomets.messenger1.routing

import com.pitomets.messenger1.dto.*
import com.pitomets.messenger1.service.ChatService
import com.pitomets.messenger1.service.MessageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.messageRoutes(
    messageService: MessageService,
    chatService: ChatService
) {
    route("/api/messages") {
        // Создать сообщение
        post {
            val request = call.receive<CreateMessageRequest>()
            val senderId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            // Проверяем, что пользователь в этом чате
            if (!chatService.isUserInChat(request.chatId, senderId)) {
                return@post call.respond(HttpStatusCode.Forbidden, "User is not in this chat")
            }

            val message = messageService.createMessage(request.chatId, senderId, request.content)
            call.respond(HttpStatusCode.Created, MessageResponse.from(message))
        }

        // Получить сообщения чата
        get("/chat/{chatId}") {
            val chatId = call.parameters["chatId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid chat ID")

            val userId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            // Проверяем, что пользователь в этом чате
            if (!chatService.isUserInChat(chatId, userId)) {
                return@get call.respond(HttpStatusCode.Forbidden, "User is not in this chat")
            }

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0

            val messages = messageService.getMessagesByChatId(chatId, limit, offset)
            call.respond(messages.map { MessageResponse.from(it) })
        }

        // Отметить сообщения как прочитанные
        put("/chat/{chatId}/read") {
            val chatId = call.parameters["chatId"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid chat ID")

            val userId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            if (!chatService.isUserInChat(chatId, userId)) {
                return@put call.respond(HttpStatusCode.Forbidden, "User is not in this chat")
            }

            messageService.markMessagesAsRead(chatId, userId)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
    }
}

