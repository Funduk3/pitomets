package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Favourites(
    @Column(name = "buyer_id")
    var buyerId: Long? = null,

    @Column(name = "listing_id")
    var listingId: Long? = null
)
