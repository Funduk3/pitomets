package com.pitomets.monolit.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "metro_lines")
class MetroLineEntity(

    @Id
    val id: Long,

    @Column(nullable = false)
    val title: String,

    @Column
    var color: String,

    @Column(name = "city_id", nullable = false)
    val cityId: Long,

    @OneToMany(
        mappedBy = "line",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val stations: MutableList<MetroStationEntity> = mutableListOf()
)
