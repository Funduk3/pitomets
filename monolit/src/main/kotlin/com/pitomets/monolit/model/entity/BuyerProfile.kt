package com.pitomets.monolit.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "buyer_profiles")
class BuyerProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: User? = null
)