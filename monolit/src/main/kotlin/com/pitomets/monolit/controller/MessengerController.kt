package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.service.MessengerService
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/messenger")
class MessengerController(
    private val messengerService: MessengerService
) {
    // Chats endpoints
    @PostMapping("/chats")
    fun createOrGetChat(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Any> {
        val result = messengerService.proxyRequest(
            userId = userPrincipal.id,
            method = HttpMethod.POST,
            path = "/api/chats",
            body = request
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/chats")
    fun getUserChats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Any> {
        val result = messengerService.proxyRequest(
            userId = userPrincipal.id,
            method = HttpMethod.GET,
            path = "/api/chats"
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/chats/{chatId}")
    fun getChat(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable chatId: Long
    ): ResponseEntity<Any> {
        val result = messengerService.proxyRequest(
            userId = userPrincipal.id,
            method = HttpMethod.GET,
            path = "/api/chats/$chatId"
        )
        return ResponseEntity.ok(result)
    }

    // Messages endpoints
    @PostMapping("/messages")
    fun createMessage(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Any> {
        val result = messengerService.proxyRequest(
            userId = userPrincipal.id,
            method = HttpMethod.POST,
            path = "/api/messages",
            body = request
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/messages/chat/{chatId}")
    fun getChatMessages(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable chatId: Long,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Long?
    ): ResponseEntity<Any> {
        val queryParams = mutableMapOf<String, String>()
        limit?.let { queryParams["limit"] = it.toString() }
        offset?.let { queryParams["offset"] = it.toString() }

        val result = messengerService.proxyRequest(
            userId = userPrincipal.id,
            method = HttpMethod.GET,
            path = "/api/messages/chat/$chatId",
            queryParams = queryParams
        )
        return ResponseEntity.ok(result)
    }

    @PutMapping("/messages/chat/{chatId}/read")
    fun markMessagesAsRead(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable chatId: Long
    ): ResponseEntity<Any> {
        val result = messengerService.proxyRequest(
            userId = userPrincipal.id,
            method = HttpMethod.PUT,
            path = "/api/messages/chat/$chatId/read"
        )
        return ResponseEntity.ok(result)
    }
}
