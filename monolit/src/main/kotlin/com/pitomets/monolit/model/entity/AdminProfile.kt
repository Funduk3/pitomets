package com.pitomets.monolit.model.entity

import jakarta.persistence.*


@Entity
@Table(name = "admin_profiles")
class AdminProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    var admin: User? = null,

//    @OneToMany(mappedBy = "adminProfile", cascade = [CascadeType.ALL])
//    var adminActions: MutableList<AdminAction> = mutableListOf()
) {
    constructor(): this(null)
}