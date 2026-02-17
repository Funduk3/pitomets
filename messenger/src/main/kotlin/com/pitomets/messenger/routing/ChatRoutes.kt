package com.pitomets.messenger.routing

import com.pitomets.messenger.dto.ChatResponse
import com.pitomets.messenger.dto.CreateChatRequest
import com.pitomets.messenger.service.BlockService
import com.pitomets.messenger.service.ChatService
import com.pitomets.messenger.service.MessageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun Route.chatRoutes(chatService: ChatService, messageService: MessageService, blockService: BlockService) {
    route("/api/chats") {
        // Создать или получить чат
        post {
            val request = call.receive<CreateChatRequest>()
            val currentUserId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            if (blockService.hasBlockBetween(currentUserId, request.userId)) {
                return@post call.respond(HttpStatusCode.Forbidden, "Chat is blocked between users")
            }

            val chat = chatService.createOrGetChat(
                currentUserId,
                request.userId,
                request.listingId,
                request.listingTitle
            )
            call.respond(HttpStatusCode.OK, ChatResponse.from(chat))
        }

        // Получить все чаты пользователя
        get {
            val userId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")

            val chats = chatService.getUserChats(userId)
            val chatIds = chats.map { it.id }
            val lastMessagesByChat = messageService.getLastMessagesByChatIds(chatIds)
            val lastUnreadMessageIds = chats.mapNotNull { chat ->
                if (userId == chat.user1Id) {
                    chat.lastUnreadMessageIdUser1
                } else {
                    chat.lastUnreadMessageIdUser2
                }
            }
            val lastUnreadMessagesById = messageService.getMessagesByIds(lastUnreadMessageIds)
                .associateBy { it.id }
            val result = chats.map { chat ->
                val unreadCount = if (userId == chat.user1Id) chat.unreadCountUser1 else chat.unreadCountUser2
                val lastUnreadMessageId = if (userId == chat.user1Id) {
                    chat.lastUnreadMessageIdUser1
                } else {
                    chat.lastUnreadMessageIdUser2
                }
                ChatResponse.from(
                    chat,
                    unreadCount = unreadCount,
                    lastMessage = lastMessagesByChat[chat.id]?.let {
                        com.pitomets.messenger.dto.MessageResponse.from(
                            it
                        )
                    },
                    lastUnreadMessage = lastUnreadMessageId?.let { lastUnreadMessagesById[it] }
                        ?.let { com.pitomets.messenger.dto.MessageResponse.from(it) }
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

            val unreadCount = if (userId == chat.user1Id) chat.unreadCountUser1 else chat.unreadCountUser2
            val lastUnreadMessageId = if (userId == chat.user1Id) {
                chat.lastUnreadMessageIdUser1
            } else {
                chat.lastUnreadMessageIdUser2
            }
            val lastUnreadMessage = lastUnreadMessageId?.let { messageService.getMessageById(it) }
            call.respond(
                ChatResponse.from(
                    chat,
                    unreadCount = unreadCount,
                    lastMessage = messageService.getLastMessageByChatId(chat.id)
                        ?.let { com.pitomets.messenger.dto.MessageResponse.from(it) },
                    lastUnreadMessage = lastUnreadMessage?.let { com.pitomets.messenger.dto.MessageResponse.from(it) }
                )
            )
        }
    }
}
