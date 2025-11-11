package com.pitomets.monolit.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

// переделать потом
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "full_name")
    var fullName: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "banned_until")
    var bannedUntil: OffsetDateTime? = null,

//    @OneToMany(mappedBy = "seller", cascade = [CascadeType.ALL], orphanRemoval = true)
//    var sellerProfiles: MutableList<SellerProfile> = mutableListOf(),
//
//    @OneToMany(mappedBy = "buyer", cascade = [CascadeType.ALL])
//    var buyerProfiles: MutableList<BuyerProfile> = mutableListOf(),
//
//    @OneToMany(mappedBy = "admin", cascade = [CascadeType.ALL])
//    var adminProfiles: MutableList<AdminProfile> = mutableListOf(),
//
//    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
//    var addresses: MutableList<Address> = mutableListOf()
)