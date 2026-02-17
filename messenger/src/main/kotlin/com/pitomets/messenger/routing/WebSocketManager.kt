package com.pitomets.messenger.routing

import com.pitomets.messenger.dto.MessageResponse
import com.pitomets.messenger.models.ReadReceiptEvent
import com.pitomets.messenger.models.SyncResponse
import com.pitomets.messenger.models.WebSocketMessage
import com.pitomets.messenger.service.ChatService
import com.pitomets.messenger.service.MessageService
import com.pitomets.messenger.service.MessagingBlockedException
import io.ktor.server.request.header
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class WebSocketManager {

    // сейчас держим максимум 1 коннект
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
            val userId = extractUserIdOrClose() ?: return@webSocket
            webSocketManager.addConnection(userId, this)

            try {
                incoming.consumeEach { frame ->
                    val text = (frame as? Frame.Text)?.readText() ?: return@consumeEach
                    val wsMessage = decodeMessageOrSendError(text) ?: return@consumeEach
                    handleMessage(wsMessage, userId, messageService, chatService, webSocketManager)
                }
            } finally {
                webSocketManager.removeConnection(userId, this)
            }
        }
    }
}

private suspend fun WebSocketServerSession.extractUserIdOrClose(): Long? {
    val userIdHeader = call.request.header("X-User-Id")
    val userIdQuery = call.request.queryParameters["userId"]
    val userId = userIdHeader?.toLongOrNull() ?: userIdQuery?.toLongOrNull()
    if (userId == null) {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing userId"))
        return null
    }
    return userId
}

private suspend fun WebSocketServerSession.decodeMessageOrSendError(text: String): WebSocketMessage? =
    try {
        Json.decodeFromString<WebSocketMessage>(text)
    } catch (_: Exception) {
        send(Frame.Text("""{"error": "Invalid message format"}"""))
        null
    }

private suspend fun WebSocketServerSession.handleMessage(
    wsMessage: WebSocketMessage,
    userId: Long,
    messageService: MessageService,
    chatService: ChatService,
    webSocketManager: WebSocketManager
) {
    when (wsMessage.type) {
        "send_message" -> handleSendMessage(wsMessage, userId, messageService, chatService, webSocketManager)
        "sync" -> handleSync(wsMessage, userId, messageService)
        else -> send(Frame.Text("""{"error": "Unknown message type"}"""))
    }
}

@Suppress("ReturnCount")
private suspend fun WebSocketServerSession.handleSendMessage(
    wsMessage: WebSocketMessage,
    userId: Long,
    messageService: MessageService,
    chatService: ChatService,
    webSocketManager: WebSocketManager
) {
    val chatId = wsMessage.chatId
    val content = wsMessage.content
    if (chatId == null || content == null) {
        send(Frame.Text("""{"error": "Missing chatId or content"}"""))
        return
    }
    if (!chatService.isUserInChat(chatId, userId)) {
        send(Frame.Text("""{"error": "User is not in this chat"}"""))
        return
    }

    val message = try {
        messageService.createMessage(chatId, userId, content)
    } catch (_: MessagingBlockedException) {
        send(Frame.Text("""{"error": "Messaging is blocked between users"}"""))
        return
    }
    val messageResponse = MessageResponse.from(message)

    // Отправляем сообщение отправителю
    send(Frame.Text(Json.encodeToString(messageResponse)))

    // Отправляем сообщение получателю
    webSocketManager.sendToChat(chatId, userId, messageResponse, chatService)
}

private suspend fun WebSocketServerSession.handleSync(
    wsMessage: WebSocketMessage,
    userId: Long,
    messageService: MessageService
) {
    val lastMessageIdsMap = wsMessage.lastMessageIds
    if (lastMessageIdsMap.isNullOrEmpty()) {
        send(Frame.Text(Json.encodeToString(SyncResponse(type = "sync_response", messages = emptyMap()))))
        return
    }

    val lastMessageIds = lastMessageIdsMap.mapNotNull { (chatIdStr, msgIdStr) ->
        val chatId = chatIdStr.toLongOrNull()
        val msgId = msgIdStr.toLongOrNull()
        if (chatId != null && msgId != null) chatId to msgId else null
    }.toMap()

    val unreadMessages = messageService.getUnreadMessagesAfter(userId, lastMessageIds)
    val responseMessages = unreadMessages
        .mapKeys { it.key.toString() }
        .mapValues { (_, messages) -> messages.map { msg -> MessageResponse.from(msg) } }

    send(Frame.Text(Json.encodeToString(SyncResponse(type = "sync_response", messages = responseMessages))))
}
