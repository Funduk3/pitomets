package com.pitomets.monolit.utils

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.User
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.UserRepo
import org.springframework.data.repository.findByIdOrNull

fun UserRepo.findUserOrThrow(userId: Long): User =
    findByIdOrNull(userId)
        ?: throw UserNotFoundException("User with id $userId not found")

fun ListingsRepo.findListingOrThrow(listingId: Long): Listing =
    findByIdOrNull(listingId)
        ?: throw ListingNotFoundException("Listing with id $listingId not found")
