package com.pitomets.messenger.routing

import com.pitomets.messenger.models.BlockStatusEvent
import com.pitomets.messenger.service.BlockService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("LongMethod")
fun Route.blockRoutes(blockService: BlockService, webSocketManager: WebSocketManager) {
    route("/api/blocks") {
        get("/{blockedId}") {
            val blockerId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")
            val blockedId = call.parameters["blockedId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid blocked ID")

            val blocked = blockService.isBlocked(blockerId, blockedId)
            call.respond(HttpStatusCode.OK, mapOf("blocked" to blocked))
        }

        get("/status/{otherId}") {
            val userId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")
            val otherId = call.parameters["otherId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid other ID")

            val blockedByMe = blockService.isBlocked(userId, otherId)
            val blockedMe = blockService.isBlocked(otherId, userId)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "blockedByMe" to blockedByMe,
                    "blockedMe" to blockedMe,
                    "blockedAny" to (blockedByMe || blockedMe)
                )
            )
        }

        post("/{blockedId}") {
            val blockerId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")
            val blockedId = call.parameters["blockedId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid blocked ID")

            blockService.blockUser(blockerId, blockedId)
            val payloadForBlocker = Json.encodeToString(
                BlockStatusEvent(
                    otherUserId = blockedId,
                    blockedByMe = true,
                    blockedMe = false,
                    blockedAny = true
                )
            )
            val payloadForBlocked = Json.encodeToString(
                BlockStatusEvent(
                    otherUserId = blockerId,
                    blockedByMe = false,
                    blockedMe = true,
                    blockedAny = true
                )
            )
            webSocketManager.sendToUser(blockerId, payloadForBlocker)
            webSocketManager.sendToUser(blockedId, payloadForBlocked)
            call.respond(HttpStatusCode.OK, mapOf("status" to "blocked"))
        }

        delete("/{blockedId}") {
            val blockerId = call.request.header("X-User-Id")?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")
            val blockedId = call.parameters["blockedId"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid blocked ID")

            blockService.unblockUser(blockerId, blockedId)
            val payloadForBlocker = Json.encodeToString(
                BlockStatusEvent(
                    otherUserId = blockedId,
                    blockedByMe = false,
                    blockedMe = false,
                    blockedAny = false
                )
            )
            val payloadForBlocked = Json.encodeToString(
                BlockStatusEvent(
                    otherUserId = blockerId,
                    blockedByMe = false,
                    blockedMe = false,
                    blockedAny = false
                )
            )
            webSocketManager.sendToUser(blockerId, payloadForBlocker)
            webSocketManager.sendToUser(blockedId, payloadForBlocked)
            call.respond(HttpStatusCode.OK, mapOf("status" to "unblocked"))
        }
    }
}
