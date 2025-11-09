package com.pitomets.monolit.controller

import com.pitomets.monolit.model.*
import com.pitomets.monolit.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val service: UserService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        return try {
            val user = User(name = request.name, password = request.password)
            val savedUser = service.register(user)
            ResponseEntity(
                UserResponse(id = savedUser.id!!, name = savedUser.name),
                HttpStatus.CREATED
            )
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        return try {
            val token = service.verify(request.name, request.password)
            ResponseEntity.ok(LoginResponse(token))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    //для теста
    @GetMapping("/all")
    fun getAll(): List<UserResponse> {
        return service.getAll().map { UserResponse(it.id, it.name) }
    }
}
