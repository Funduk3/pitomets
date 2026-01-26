package com.pitomets.monolit.repository

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.entity.SellerProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
interface SellerProfileRepo : JpaRepository<SellerProfile, Long> {
    fun findBySellerId(sellerId: Long): SellerProfile?

    fun findBySellerIdOrThrow(sellerId: Long): SellerProfile =
        findByIdOrNull(sellerId)
            ?: throw UserNotFoundException("User with id $sellerId not found")
}
