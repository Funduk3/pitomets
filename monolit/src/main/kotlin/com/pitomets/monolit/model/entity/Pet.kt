package com.pitomets.monolit.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "pets")
class Pet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    var species: String? = null,
    var breed: String? = null,
    var age: Int? = null,
    var weight: Double? = null,

    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @Column(columnDefinition = "text")
    var description: String? = null,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "listing_id")
//    var listing: Listing? = null
)