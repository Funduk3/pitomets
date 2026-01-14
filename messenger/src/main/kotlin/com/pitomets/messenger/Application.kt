package com.pitomets.messenger

import com.pitomets.messenger.db.DatabaseFactory
import com.pitomets.messenger.routing.*
import com.pitomets.messenger.routing.WebSocketManager
import com.pitomets.messenger.service.ChatService
import com.pitomets.messenger.service.MessageService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Инициализация базы данных
    DatabaseFactory.init()

    // Инициализация сервисов
    val messageService = MessageService()
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
        chatRoutes(chatService, messageService)
        webSocketRoutes(messageService, chatService, webSocketManager)
    }
}
