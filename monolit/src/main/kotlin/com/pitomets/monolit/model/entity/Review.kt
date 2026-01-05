package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "reviews")
class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "rating", nullable = false)
    var rating: Int,

    @Column(name = "stars_number", nullable = false)
    var starsNumber: Int = 0,

    @Column(columnDefinition = "text")
    var text: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    var author: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    var sellerProfile: SellerProfile,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    var listing: Listing? = null,
) {
    @PrePersist
    fun fillStarsAndRating() {
        if (starsNumber == 0) starsNumber = rating
    }
}
