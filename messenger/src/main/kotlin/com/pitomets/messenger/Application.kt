package com.pitomets.messenger

import com.pitomets.messenger.db.DatabaseFactory
import com.pitomets.messenger.routing.WebSocketManager
import com.pitomets.messenger.routing.blockRoutes
import com.pitomets.messenger.routing.chatRoutes
import com.pitomets.messenger.routing.messageRoutes
import com.pitomets.messenger.routing.webSocketRoutes
import com.pitomets.messenger.service.BlockService
import com.pitomets.messenger.service.ChatService
import com.pitomets.messenger.service.MessageService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Инициализация базы данных
    DatabaseFactory.init()

    // Инициализация сервисов
    val blockService = BlockService()
    val messageService = MessageService(blockService)
    val chatService = ChatService()
    val webSocketManager = WebSocketManager()

    // Установка WebSockets
    install(WebSockets)

    // Настройка CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-User-Id")
        anyHost()
    }

    // Настройка JSON сериализации
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }

    // Обработка ошибок
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Internal server error"))
            )
        }
    }

    // Роутинг
    routing {
        get("/") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "Messenger API is running"))
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }

        messageRoutes(messageService, chatService, webSocketManager)
        chatRoutes(chatService, messageService, blockService)
        blockRoutes(blockService, webSocketManager)
        webSocketRoutes(messageService, chatService, webSocketManager)
    }
}
