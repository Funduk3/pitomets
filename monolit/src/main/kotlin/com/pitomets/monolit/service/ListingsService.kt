package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ListingsService(
    private val petsRepo: PetsRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo
) {
    private val log = LoggerFactory.getLogger(ListingsService::class.java)

    fun createListing(
        userId: Long,
        request: ListingsRequest
    ): ListingsResponse {
        val seller = sellerProfileRepo.findById(userId).orElseThrow {
            UserNotFoundException("User not found")
        }

        val father = request.father?.let { id ->
            petsRepo.findById(id).orElse(null)
        }
        val mother = request.mother?.let { id ->
            petsRepo.findById(id).orElse(null) // преобразуем Optional в null
        }

        val listing = Listing(
            description = request.description,
            species = request.species,
            breed = request.breed,
            ageMonths = request.ageMonths,
            father = father,
            mother = mother,
            price = request.price,
            sellerProfile = seller
        )

        listingsRepo.save(listing)
        log.info("Created Listing: {}", listing)

        return ListingsResponse(
            description = listing.description,
            species = listing.species,
            breed = listing.breed,
            ageMonths = listing.ageMonths,
            father = father?.id,
            mother = mother?.id,
            listingsId = requireNotNull(listing.id),
            price = listing.price,
            isArchived = listing.isArchived,
        )
    }
}
