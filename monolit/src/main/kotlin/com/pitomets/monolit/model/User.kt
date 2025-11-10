package com.pitomets.monolit.model

import jakarta.persistence.*

// переделать потом
@Entity
@Table(name = "users")
data class User (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String,

    val password: String,
)
