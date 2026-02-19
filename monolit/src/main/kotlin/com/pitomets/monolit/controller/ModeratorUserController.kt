package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.dto.response.UserResponse
import com.pitomets.monolit.service.ListingsService
import com.pitomets.monolit.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/user")
class ModeratorUserController(
    private val userService: UserService
) {
    @GetMapping("/all")
    fun getAllPendingUsers(): List<UserResponse> =
        userService.getPendingUsers()


    @GetMapping("/{id}")
    fun getPendingUserById(@PathVariable id: Long) =
        userService.getPendingUser(id)

    @PostMapping("/{id}")
    fun acceptUser(@PathVariable id: Long) {
        userService.approveUser(id)
    }

    @PostMapping("/{id}")
    fun declineUser(
        @PathVariable id: Long,
        @RequestBody adminMessage: AdminMessage
    ) {
        userService.declineUser(id, adminMessage)
    }
}