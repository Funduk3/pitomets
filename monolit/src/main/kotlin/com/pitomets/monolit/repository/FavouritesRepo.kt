package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.Favourite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FavouritesRepo : JpaRepository<Favourite, Long> {
    fun findAllByUserId(userId: Long): List<Favourite>
    fun findByUserIdAndListingId(userId: Long, itemId: Long): Favourite?
    fun deleteAllByListingId(listingId: Long)
}
