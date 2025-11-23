package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "listing")
class Listing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    var species: String? = null,
    var breed: String? = null,

    @Column(name = "age_months")
    var ageMonths: Int? = null,

    // mother/father reference to Pet (FK to pets.id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mother")
    var mother: Pet? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "father")
    var father: Pet? = null,

    var price: Int? = null,

    @Column(name = "is_archived")
    var isArchived: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    var sellerProfile: SellerProfile? = null,

//    @OneToMany(mappedBy = "listing", cascade = [CascadeType.ALL], orphanRemoval = true)
//    var pets: MutableList<Pet> = mutableListOf()
) {
    constructor() : this(null)
}
