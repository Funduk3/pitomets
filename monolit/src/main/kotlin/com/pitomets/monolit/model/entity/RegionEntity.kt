package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "regions")
class RegionEntity(

    @Id
    val id: Long,

    @Column(name = "country_id", nullable = false)
    val countryId: Long,

    @Column(nullable = false)
    val title: String
)
