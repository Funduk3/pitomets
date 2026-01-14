package com.pitomets.messenger.routing

import com.pitomets.messenger.dto.CreateMessageRequest
import com.pitomets.messenger.dto.MessageResponse
import com.pitomets.messenger.service.ChatService
import com.pitomets.messenger.service.MessageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private const val DEFAULT_MESSAGES_LIMIT = 50

fun Route.messageRoutes(
    messageService: MessageService,
    chatService: ChatService,
    webSocketManager: WebSocketManager
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

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_MESSAGES_LIMIT
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
            // Уведомляем второго участника, что чат прочитан (для ✓✓)
            webSocketManager.sendReadReceipt(chatId, userId, chatService)
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
    }
}
