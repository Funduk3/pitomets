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

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    var seller: User,

    @Column(name = "shop_name")
    var shopName: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column
    var rating: Double = 0.0,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false

//    @OneToMany(mappedBy = "sellerProfile", cascade = [CascadeType.ALL], orphanRemoval = true)
//    var listings: MutableList<Listing> = mutableListOf(),
//
//    @OneToMany(mappedBy = "sellerProfile", cascade = [CascadeType.ALL])
//    var reviews: MutableList<Review> = mutableListOf()
)
