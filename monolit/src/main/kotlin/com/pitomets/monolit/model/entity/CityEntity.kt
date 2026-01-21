package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "cities",
    indexes = [
        Index(name = "idx_cities_title", columnList = "title")
    ]
)
class CityEntity(

    @Id
    val id: Long,

    @Column(name = "region_id", nullable = false)
    val regionId: Long,

    @Column(name = "country_id", nullable = false)
    val idCountry: Long,

    @Column(nullable = false)
    val title: String,

    @Column(name = "title_eng", nullable = false)
    val titleEng: String,
)
