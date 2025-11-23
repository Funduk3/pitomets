package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "admin_actions")
class AdminAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    var admin: User? = null,

    // seller_profile reference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    var sellerProfile: SellerProfile? = null,

    @Column(columnDefinition = "text")
    var reason: String? = null,

    var type: String? = null,

    @Column(name = "acted_at")
    var actedAt: OffsetDateTime? = OffsetDateTime.now()
)
