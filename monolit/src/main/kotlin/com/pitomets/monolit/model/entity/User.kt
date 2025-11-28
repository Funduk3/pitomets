package com.pitomets.monolit.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    var id: Long? = null,

    @Column(unique = true)
    var email: String? = null,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "full_name")
    var fullName: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "banned_until")
    var bannedUntil: OffsetDateTime? = null,

    @OneToOne(mappedBy = "seller", cascade = [CascadeType.ALL], orphanRemoval = true)
    var sellerProfile: SellerProfile? = null,

    @OneToOne(mappedBy = "buyer", cascade = [CascadeType.ALL], orphanRemoval = true)
    var buyerProfile: BuyerProfile? = null,

    @OneToOne(mappedBy = "admin", cascade = [CascadeType.ALL], orphanRemoval = true)
    var adminProfile: AdminProfile? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var address: Address? = null
)
