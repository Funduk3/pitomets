package com.pitomets.monolit.repository

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
interface UserRepo : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByConfirmationToken(token: String): User?
    fun findByPasswordResetToken(token: String): User?
    fun findByIsApprovedFalse(): List<User>
    fun findByIdAndIsApprovedFalse(id: Long): User?
    fun findByAvatarKeyIsNotNull(): List<User>
}

fun UserRepo.findUserOrThrow(userId: Long): User =
    findByIdOrNull(userId)
        ?: throw UserNotFoundException("User with id $userId not found")
