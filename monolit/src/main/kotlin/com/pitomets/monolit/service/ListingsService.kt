package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.exceptions.profileExceptions.InvalidRoleException
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.UserRole
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.UserRepo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ListingsService(
    private val userRepo: UserRepo,
    private val petsRepo: PetsRepo,
    private val listingsRepo: ListingsRepo,
) {
    private val log = LoggerFactory.getLogger(ListingsService::class.java)

    fun createListing(
        userId: Long,
        request: ListingsRequest
    ): ListingsResponse {
        val user = userRepo.findById(userId).orElseThrow {
            UserNotFoundException("User not found")
        }
        if (user.role != UserRole.SELLER) {
            throw InvalidRoleException("User is not seller")
        }
        val father = if (request.father != null) {
            petsRepo.findByid(request.father)
        } else {
            null
        }
        val mother = if (request.mother != null) {
            petsRepo.findByid(request.mother)
        } else {
            null
        }
        val listing = Listing(
            description = request.description,
            species = request.species,
            breed = request.breed,
            ageMonths = request.ageMonths,
            father = father,
            mother = mother,
            price = request.price,
        )
        listingsRepo.save(listing)
        log.info("Created Listings: {}", listing)

        return ListingsResponse(
            description = listing.description,
            species = listing.species,
            breed = listing.breed,
            ageMonths = listing.ageMonths,
            father = request.father,
            mother = request.mother,
            listingsId = requireNotNull(listing.id),
            price = listing.price,
            isArchived = listing.isArchived,
        )
    }
}
