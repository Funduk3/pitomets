package com.pitomets.monolit.model.entity

import com.pitomets.monolit.model.Gender
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// Этот класс по факту дублирует listing,
// Мы его использовали только для mother/father,
// По факту нигде не используется
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
