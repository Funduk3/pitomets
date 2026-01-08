package com.messenger

import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.features.StatusPages
import io.ktor.application.install
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.request.receive
import io.ktor.websocket.webSocket
import io.ktor.server.websockets.WebSockets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.features.ContentNegotiation

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true })
    }

    install(WebSockets)

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.localizedMessage)
        }
    }

    routing {
        get("/") {
            call.respond(HttpStatusCode.OK, "Messenger API is running")
        }

        route("/chat") {
            webSocket {
                send("Welcome to the WebSocket chat!")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    send("You said: $receivedText")
                }
            }
        }
    }
}
