package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "listing_photos")
class ListingPhoto(
    @Id
    @GeneratedValue
    val id: Long = 0,

    @Column(nullable = false, name = "listing_id")
    val listingId: Long,

    @Column(nullable = false, name = "object_key")
    var objectKey: String,

    @Column(nullable = false)
    var position: Int
)
