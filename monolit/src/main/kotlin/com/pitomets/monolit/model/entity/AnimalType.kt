package com.pitomets.monolit.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "animal_type")
class AnimalType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val hasBreed: Boolean,
) {
    @OneToMany(mappedBy = "animalType", cascade = [CascadeType.ALL], orphanRemoval = true)
    var breeds: MutableList<Breed> = mutableListOf()
}
