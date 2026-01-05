package com.pitomets.monolit.utils

import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.Pet
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.service.SearchService
import org.slf4j.Logger

internal fun findSellerProfile(
    userId: Long,
    sellerProfileRepo: SellerProfileRepo,
    log: Logger
): SellerProfile {
    val seller = sellerProfileRepo.findBySellerId(userId)
    if (seller == null) {
        log.error("Seller profile not found for user ID: {}", userId)
        throw UserNotFoundException("User with seller id $userId does not exist")
    }
    log.info("Found seller profile: ID={}, shopName={}", seller.id, seller.shopName)
    return seller
}

internal fun findParentPets(
    request: ListingsRequest,
    petsRepo: PetsRepo,
    log: Logger
): Pair<Pet?, Pet?> {
    val father = request.father?.let { id ->
        log.debug("Looking up father pet with ID: {}", id)
        petsRepo.findById(id).orElse(null)
    }
    val mother = request.mother?.let { id ->
        log.debug("Looking up mother pet with ID: {}", id)
        petsRepo.findById(id).orElse(null)
    }
    return Pair(father, mother)
}

internal fun createListingEntity(
    request: ListingsRequest,
    seller: SellerProfile,
    father: Pet?,
    mother: Pet?
) = Listing(
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

internal fun saveListing(
    listing: Listing,
    listingsRepo: ListingsRepo,
    log: Logger
): Listing {
    log.info("Saving listing to database...")
    val saved = listingsRepo.save(listing)
    log.info("Listing saved with ID: {}", saved.id)
    return saved
}

internal fun indexListingInElasticsearch(
    savedListing: Listing,
    searchService: SearchService,
    log: Logger
) {
    val listingId = requireNotNull(savedListing.id) { "Listing ID is null after save" }
    log.info("Indexing listing in Elasticsearch with ID: {}", listingId)

    searchService.indexListing(
        SearchListingDocument(
            id = listingId,
            description = savedListing.description,
            title = savedListing.title
        )
    )
    log.info("Successfully indexed listing in Elasticsearch")
}

internal fun buildListingsResponse(
    savedListing: Listing,
    father: Pet?,
    mother: Pet?
) = ListingsResponse(
    description = savedListing.description,
    sellerId = requireNotNull(savedListing.sellerProfile.seller?.id),
    sellerRating = savedListing.sellerProfile.rating,
    sellerReviewsCount = savedListing.sellerProfile.countReviews,
    species = savedListing.species,
    breed = savedListing.breed,
    ageMonths = savedListing.ageMonths,
    father = father?.id,
    mother = mother?.id,
    listingsId = requireNotNull(savedListing.id),
    price = savedListing.price,
    isArchived = savedListing.isArchived,
    title = savedListing.title
)
