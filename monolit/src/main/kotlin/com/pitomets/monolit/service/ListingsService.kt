package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.PetNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.utils.buildListingsResponse
import com.pitomets.monolit.utils.createListingEntity
import com.pitomets.monolit.utils.findListingOrThrow
import com.pitomets.monolit.utils.findParentPets
import com.pitomets.monolit.utils.findSellerProfile
import com.pitomets.monolit.utils.indexListingInElasticsearch
import com.pitomets.monolit.utils.saveListing
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException
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

    fun requireOwner(listingId: Long, userId: Long) {
        val listing = listingsRepo.findListingOrThrow(listingId)

        if (listing.sellerProfile.seller?.id != userId) {
            throw AccessDeniedException(
                "User $userId is not owner of listing $listingId"
            )
        }
    }

    @Transactional
    fun createListing(
        userId: Long,
        request: ListingsRequest
    ): ListingsResponse {
        log.info("Creating listing for user ID: {}, request: {}", userId, request)

        val seller = findSellerProfile(userId, sellerProfileRepo, log)
        val (father, mother) = findParentPets(request, petsRepo, log)

        log.info(
            "Creating listing entity with title: {}, species: {}, price: {}",
            request.title,
            request.species,
            request.price
        )

        val listing = createListingEntity(request, seller, father, mother)
        val savedListing = saveListing(listing, listingsRepo, log)
        indexListingInElasticsearch(savedListing, searchService, log)

        log.info("Successfully created listing with ID: {}, title: {}", savedListing.id, savedListing.title)

        return buildListingsResponse(savedListing, father, mother)
    }

    fun getListing(
        listingId: Long
    ): ListingsResponse {
        val response = listingsRepo.findListingOrThrow(listingId)
        return ListingsResponse(
            description = response.description,
            sellerId = requireNotNull(response.sellerProfile.seller?.id),
            sellerRating = response.sellerProfile.rating,
            sellerReviewsCount = response.sellerProfile.countReviews,
            species = response.species,
            breed = response.breed,
            ageMonths = response.ageMonths,
            mother = response.mother?.id,
            father = response.father?.id,
            price = response.price,
            isArchived = response.isArchived,
            listingsId = listingId,
            title = response.title
        )
    }

    fun getUserListings(userId: Long): List<ListingsResponse> {
        val seller = findSellerProfile(userId, sellerProfileRepo, log)
        val listings = listingsRepo.findBySellerProfile(seller)
        return listings.map { listing ->
            ListingsResponse(
                description = listing.description,
                sellerId = requireNotNull(listing.sellerProfile.seller?.id),
                sellerRating = listing.sellerProfile.rating,
                sellerReviewsCount = listing.sellerProfile.countReviews,
                species = listing.species,
                breed = listing.breed,
                ageMonths = listing.ageMonths,
                mother = listing.mother?.id,
                father = listing.father?.id,
                price = listing.price,
                isArchived = listing.isArchived,
                listingsId = requireNotNull(listing.id),
                title = listing.title
            )
        }
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
            sellerId = sellerId,
            sellerRating = updatedListing.sellerProfile.rating,
            sellerReviewsCount = updatedListing.sellerProfile.countReviews,
            species = updatedListing.species,
            ageMonths = updatedListing.ageMonths,
            price = updatedListing.price,
            breed = updatedListing.breed,
            isArchived = updatedListing.isArchived,
            listingsId = listingId,
            mother = updatedListing.mother?.id,
            father = updatedListing.father?.id,
            title = updatedListing.title
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
