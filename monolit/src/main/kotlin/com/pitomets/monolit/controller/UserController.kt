package com.pitomets.monolit.controller

import com.pitomets.monolit.model.User
import com.pitomets.monolit.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class UserController {
    @Autowired
    private val service: UserService? = null


    @PostMapping("/register")
    fun register(@RequestBody user: User): User {
        return service!!.register(user)
    }

    @PostMapping("/login")
    fun login(@RequestBody user: User): String? {
        return service!!.verify(user)
    }
}