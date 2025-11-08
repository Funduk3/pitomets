package com.pitomets.monolit.model

import jakarta.persistence.*;

@Entity
@Table(name = "users")
data class User (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long ?= null,

    var name: String,

    var password: String
)