package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.SellerProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ListingsRepo : JpaRepository<Listing, Long> {
    fun findAllByIdIn(ids: List<Long>): List<Listing>
    fun findBySellerProfile(sellerProfile: SellerProfile): List<Listing>
}
