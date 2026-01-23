package com.pitomets.monolit.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "metro_stations")
class MetroStationEntity(

    @Id
    val id: Long,

    @Column(nullable = false)
    val title: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    val line: MetroLineEntity
)
