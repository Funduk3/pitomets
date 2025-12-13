package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "seller_profiles")
class SellerProfile(
    @Id
    @Column(name = "id")
    var id: Long? = null,

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    var seller: User? = null,

    @Column(name = "shop_name")
    var shopName: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column
    var rating: Double = 0.0,

    @Column(name = "count_reviews")
    var countReviews: Long = 0,

    @Column(name = "sum_reviews")
    var sumReviews: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false
)
