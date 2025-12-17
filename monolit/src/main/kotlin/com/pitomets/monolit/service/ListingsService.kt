package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.PetNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.utils.findListingOrThrow
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Service
class ListingsService(
    private val petsRepo: PetsRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val searchService: SearchService,
    private val executor: ExecutorService,
) {
    private val log = LoggerFactory.getLogger(ListingsService::class.java)

    @Transactional
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
            sellerProfile = seller,
            title = request.title
        )

        listingsRepo.save(listing)
        searchService.indexListing(
            SearchListingDocument(
                id = requireNotNull(listing.id),
                description = listing.description,
                title = listing.title,
            )
        )

        log.info("Created Listing and add in elastic: {}", listing)

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

    fun getListing(
        listingId: Long
    ): ListingsResponse {
        val response = listingsRepo.findListingOrThrow(listingId)
        return ListingsResponse(
            description = response.description,
            species = response.species,
            breed = response.breed,
            ageMonths = response.ageMonths,
            mother = response.mother?.id,
            father = response.father?.id,
            price = response.price,
            isArchived = response.isArchived,
            listingsId = listingId
        )
    }

    @Transactional
    fun updateListing(
        listingId: Long,
        sellerId: Long,
        request: UpdateListingRequest
    ): ListingsResponse {
        val listing = listingsRepo.findListingOrThrow(listingId)

        if (listing.sellerProfile.seller?.id != sellerId) {
            throw UserNotFoundException(
                "User with seller id $sellerId does not has this listing," +
                    "excepted id ${listing.sellerProfile.seller?.id}"
            )
        }
        request.title?.let { listing.title = it }
        request.species?.let { listing.species = it }
        request.price?.let { listing.price = it }
        request.ageMonths?.let { listing.ageMonths = it }
        request.mother?.let {
            listing.mother = petsRepo.findById(it).orElseThrow {
                PetNotFoundException("Mother with id $it")
            }
        }
        request.father?.let {
            listing.father = petsRepo.findById(it).orElseThrow {
                PetNotFoundException("Father with id $it")
            }
        }
        request.breed?.let { listing.breed = it }
        request.isArchived?.let { listing.isArchived = it }
        request.description?.let { listing.description = it }

        val shouldIndex = request.description != null || request.title != null

        val saveFuture = CompletableFuture.supplyAsync({ listingsRepo.save(listing) }, executor)
        val indexFuture = if (shouldIndex) {
            CompletableFuture.runAsync({
                searchService.indexListing(
                    SearchListingDocument(
                        id = listingId,
                        title = listing.title,
                        description = listing.description
                    )
                )
            }, executor)
        } else {
            null
        }
        val updatedListing = saveFuture.join()
        indexFuture?.join()

        return ListingsResponse(
            description = updatedListing.description,
            species = updatedListing.species,
            ageMonths = updatedListing.ageMonths,
            price = updatedListing.price,
            breed = updatedListing.breed,
            isArchived = updatedListing.isArchived,
            listingsId = listingId,
            mother = updatedListing.mother?.id,
            father = updatedListing.father?.id,
        )
    }

    @Transactional
    fun deleteListing(
        listingId: Long,
        userId: Long
    ) {
        val listing = listingsRepo.findListingOrThrow(listingId)

        if (listing.sellerProfile.seller?.id != userId) {
            throw UserNotFoundException("User is not seller of this listing")
        }

        val deleteDb = CompletableFuture.runAsync({ listingsRepo.delete(listing) }, executor)
        val deleteIndex = CompletableFuture.runAsync({
            searchService.deleteListing(listingId)
        }, executor)

        deleteDb.join()
        deleteIndex.join()
    }
}
