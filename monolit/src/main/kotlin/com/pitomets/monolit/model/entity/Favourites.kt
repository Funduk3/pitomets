package com.pitomets.monolit.model.entity

import jakarta.persistence.*

@Embeddable
class Favouritesd(
    @Column(name = "buyer_id")
    var buyerId: Long? = null,

    @Column(name = "listing_id")
    var listingId: Long? = null
)