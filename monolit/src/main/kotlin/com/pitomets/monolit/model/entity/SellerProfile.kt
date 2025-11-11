package com.pitomets.monolit.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "seller_profiles")
class SellerProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    var seller: User? = null,

    @Column(name = "shop_name")
    var shopName: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(precision = 10, scale = 2)
    var rating: Int? = null,

//    @OneToMany(mappedBy = "sellerProfile", cascade = [CascadeType.ALL], orphanRemoval = true)
//    var listings: MutableList<Listing> = mutableListOf(),
//
//    @OneToMany(mappedBy = "sellerProfile", cascade = [CascadeType.ALL])
//    var reviews: MutableList<Review> = mutableListOf()
)