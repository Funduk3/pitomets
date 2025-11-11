package com.pitomets.monolit.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "reviews")
class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    var reviewer: User? = null,

    @Column(name = "stars_number", nullable = false)
    var starsNumber: Short = 1, // 1..5

    @Column(columnDefinition = "text")
    var message: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    var sellerProfile: SellerProfile? = null
)