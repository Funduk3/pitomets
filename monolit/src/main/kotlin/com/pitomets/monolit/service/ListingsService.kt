package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
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
        val seller = sellerProfileRepo.findBySellerId(userId)
            ?: throw UserNotFoundException("User with seller id $userId does not exist")

        val father = request.father?.let { id ->
            petsRepo.findById(id).orElse(null)
        }
        val mother = request.mother?.let { id ->
            petsRepo.findById(id).orElse(null)
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
            father = father,
            mother = mother,
            listingsId = requireNotNull(listing.id),
            price = listing.price,
            isArchived = listing.isArchived,
        )
    }

    fun getListing(
        listingId: Long
    ): ListingsResponse {
        val response = listingsRepo.findByIdOrNull(listingId)
            ?: throw ListingNotFoundException("Listing with id $listingId does not exist")
        return ListingsResponse(
            description = response.description,
            species = response.species,
            breed = response.breed,
            ageMonths = response.ageMonths,
            mother = response.mother,
            father = response.father,
            price = response.price,
            isArchived = response.isArchived,
            listingsId = listingId
        )
    }

    fun updateListing(
        listingId: Long,
        sellerId: Long,
        request: UpdateListingRequest
    ): ListingsResponse {
        val listing = listingsRepo.findByIdOrNull(listingId)
            ?: throw ListingNotFoundException("Listing does not exist")
        if (listing.sellerProfile.seller?.id != sellerId) {
            throw UserNotFoundException(
                "User with seller id $sellerId does not has this listing," +
                    "excepted id ${listing.sellerProfile.seller?.id}"
            )
        }

        request.species?.let { listing.species = it }
        request.price?.let { listing.price = it }
        request.ageMonths?.let { listing.ageMonths = it }
        request.mother?.let { listing.mother = it }
        request.father?.let { listing.father = it }
        request.breed?.let { listing.breed = it }
        request.isArchived?.let { listing.isArchived = it }

        val updatedListing = listingsRepo.save(listing)

        return ListingsResponse(
            description = updatedListing.description,
            species = updatedListing.species,
            ageMonths = updatedListing.ageMonths,
            price = updatedListing.price,
            breed = updatedListing.breed,
            isArchived = updatedListing.isArchived,
            listingsId = listingId,
            mother = updatedListing.mother,
            father = updatedListing.father,
        )
    }
}
