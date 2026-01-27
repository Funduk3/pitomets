package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.SellerProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SellerProfileRepo : JpaRepository<SellerProfile, Long> {
    @Suppress("FunctionNaming")
    fun findBySeller_Id(sellerId: Long): SellerProfile?
}
