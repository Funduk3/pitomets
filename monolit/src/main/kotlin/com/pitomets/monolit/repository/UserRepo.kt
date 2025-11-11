package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepo: JpaRepository<User, Long> {
    fun findByName(username: String): User?
}
