package com.pitomets.messenger1.routing

import com.pitomets.messenger1.dto.MessageResponse
import com.pitomets.messenger1.models.ReadReceiptEvent
import com.pitomets.messenger1.models.WebSocketMessage
import com.pitomets.messenger1.service.ChatService
import com.pitomets.messenger1.service.MessageService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.concurrent.ConcurrentHashMap

class WebSocketManager {
    /**
     * Важно: в dev (и при глюках сети) клиент может открыть 2 WS-сессии на одного юзера.
     * Тогда одно сообщение приходит в UI два раза, хотя в БД оно одно.
     *
     * Поэтому держим ровно ОДНО активное соединение на пользователя (последнее wins).
     */
    private val connections = ConcurrentHashMap<Long, WebSocketServerSession>()

    suspend fun addConnection(userId: Long, session: WebSocketServerSession) {
        // Закрываем старую сессию, если была
        val prev = connections.put(userId, session)
        if (prev != null && prev !== session) {
            try {
                prev.close(CloseReason(CloseReason.Codes.NORMAL, "Replaced by a new connection"))
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun removeConnection(userId: Long, session: WebSocketServerSession) {
        // удаляем только если это текущая сессия (иначе можно снести новую)
        connections.remove(userId, session)
    }

    suspend fun sendToUser(userId: Long, message: String) {
        val session = connections[userId] ?: return
        try {
            session.send(Frame.Text(message))
        } catch (_: Exception) {
            removeConnection(userId, session)
        }
    }

    suspend fun sendToChat(chatId: Long, senderId: Long, message: MessageResponse, chatService: ChatService) {
        val chat = chatService.getChatById(chatId) ?: return
        val recipientId = if (chat.user1Id == senderId) chat.user2Id else chat.user1Id
        
        val jsonMessage = Json.encodeToString(message)
        sendToUser(recipientId, jsonMessage)
    }

    suspend fun sendReadReceipt(chatId: Long, readerId: Long, chatService: ChatService) {
        val chat = chatService.getChatById(chatId) ?: return
        val recipientId = if (chat.user1Id == readerId) chat.user2Id else chat.user1Id
        val payload = Json.encodeToString(
            ReadReceiptEvent(
                type = "read_receipt",
                chatId = chatId,
                readerId = readerId
            )
        )
        sendToUser(recipientId, payload)
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

