package com.pitomets.messenger1.routing

import com.pitomets.messenger1.dto.MessageResponse
import com.pitomets.messenger1.service.ChatService
import com.pitomets.messenger1.service.MessageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class WebSocketMessage(
    val type: String,
    val chatId: Long? = null,
    val content: String? = null,
    val senderId: Long? = null
)

class WebSocketManager {
    private val connections = mutableMapOf<Long, MutableSet<WebSocketServerSession>>()

    fun addConnection(userId: Long, session: WebSocketServerSession) {
        connections.getOrPut(userId) { mutableSetOf() }.add(session)
    }

    fun removeConnection(userId: Long, session: WebSocketServerSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
    }

    suspend fun sendToUser(userId: Long, message: String) {
        connections[userId]?.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                removeConnection(userId, session)
            }
        }
    }

    suspend fun sendToChat(chatId: Long, senderId: Long, message: MessageResponse, chatService: ChatService) {
        val chat = chatService.getChatById(chatId) ?: return
        val recipientId = if (chat.user1Id == senderId) chat.user2Id else chat.user1Id
        
        val jsonMessage = Json.encodeToString(message)
        sendToUser(recipientId, jsonMessage)
    }
}

fun Route.webSocketRoutes(
    messageService: MessageService,
    chatService: ChatService,
    webSocketManager: WebSocketManager
) {
    route("/ws") {
        webSocket("/chat") {
            // Пытаемся получить userId из заголовка или query параметра
            val userIdHeader = call.request.headers["X-User-Id"]
            val userIdQuery = call.request.queryParameters["userId"]
            val userId = (userIdHeader?.toLongOrNull() ?: userIdQuery?.toLongOrNull())
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing userId"))
                return@webSocket
            }

            webSocketManager.addConnection(userId, this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val wsMessage = try {
                            Json.decodeFromString<WebSocketMessage>(text)
                        } catch (e: Exception) {
                            send(Frame.Text("""{"error": "Invalid message format"}"""))
                            return@consumeEach
                        }

                        when (wsMessage.type) {
                            "send_message" -> {
                                val chatId = wsMessage.chatId
                                val content = wsMessage.content

                                if (chatId == null || content == null) {
                                    send(Frame.Text("""{"error": "Missing chatId or content"}"""))
                                    return@consumeEach
                                }

                                val chatIdLong: Long = chatId
                                val contentString: String = content

                                if (!chatService.isUserInChat(chatIdLong, userId)) {
                                    send(Frame.Text("""{"error": "User is not in this chat"}"""))
                                    return@consumeEach
                                }

                                val message = messageService.createMessage(chatIdLong, userId, contentString)
                                val messageResponse = MessageResponse.from(message)

                                // Отправляем сообщение отправителю
                                send(Frame.Text(Json.encodeToString(messageResponse)))

                                // Отправляем сообщение получателю
                                webSocketManager.sendToChat(chatIdLong, userId, messageResponse, chatService)
                            }
                            else -> {
                                send(Frame.Text("""{"error": "Unknown message type"}"""))
                            }
                        }
                    }
                }
            } finally {
                webSocketManager.removeConnection(userId, this)
            }
        }
    }
}

