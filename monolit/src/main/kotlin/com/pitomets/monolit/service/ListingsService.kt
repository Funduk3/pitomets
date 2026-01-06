package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.PetNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.dto.SearchListingDocument
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.Pet
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.utils.findListingOrThrow
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Service
@Suppress("TooManyFunctions")
class ListingsService(
    private val petsRepo: PetsRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val searchService: SearchService,
    private val executor: ExecutorService,
) {
    // Controller functions

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
        return buildListingsResponse(
            response,
            response.father,
            response.mother
        )
    }

    fun getUserListings(userId: Long): List<ListingsResponse> {
        val seller = findSellerProfile(userId, sellerProfileRepo, log)
        val listings = listingsRepo.findBySellerProfile(seller)
        return listings.map { listing ->
            buildListingsResponse(
                listing,
                listing.father,
                listing.mother
            )
        }
    }

    @Transactional
    @Suppress("LongMethod")
    fun updateListing(
        listingId: Long,
        sellerId: Long,
        request: UpdateListingRequest
    ): ListingsResponse {
        val listing = requireOwnerAndReturnListing(listingId, sellerId)

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
        val sellerIdValue = requireNotNull(listing.sellerProfile.seller?.id)
        val sellerRating = listing.sellerProfile.rating
        val sellerReviewsCount = listing.sellerProfile.countReviews

        val saveFuture = CompletableFuture.supplyAsync({
            val saved = listingsRepo.save(listing)

            ListingsResponse(
                description = saved.description,
                sellerId = sellerIdValue,
                sellerRating = sellerRating,
                sellerReviewsCount = sellerReviewsCount,
                species = saved.species,
                ageMonths = saved.ageMonths,
                price = saved.price,
                breed = saved.breed,
                isArchived = saved.isArchived,
                listingsId = listingId,
                mother = saved.mother?.id,
                father = saved.father?.id,
                title = saved.title
            )
        }, executor)

        var indexFuture: CompletableFuture<*>? = null
        if (shouldIndex) {
            indexFuture = CompletableFuture.runAsync({
                searchService.indexListing(
                    SearchListingDocument(
                        id = listingId,
                        title = listing.title,
                        description = listing.description
                    )
                )
            }, executor)
        }

        val response = saveFuture.join()
        indexFuture?.join()
        return response
    }

    @Transactional
    fun deleteListing(
        listingId: Long,
        userId: Long
    ) {
        val listing = requireOwnerAndReturnListing(listingId, userId)

        val deleteDb = CompletableFuture.runAsync({ listingsRepo.delete(listing) }, executor)
        val deleteIndex = CompletableFuture.runAsync({
            searchService.deleteListing(listingId)
        }, executor)

        deleteDb.join()
        deleteIndex.join()
    }

    // use it in any class

    fun requireOwnerAndReturnListing(listingId: Long, userId: Long): Listing {
        val listing = listingsRepo.findListingOrThrow(listingId)

        if (listing.sellerProfile.seller?.id != userId) {
            throw AccessDeniedException(
                "User $userId is not owner of listing $listingId"
            )
        }
        return listing
    }

    // private methods (you can do it public if you need)

    private fun findSellerProfile(
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

    private fun findParentPets(
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

    private fun createListingEntity(
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

    private fun saveListing(
        listing: Listing,
        listingsRepo: ListingsRepo,
        log: Logger
    ): Listing {
        log.info("Saving listing to database...")
        val saved = listingsRepo.save(listing)
        log.info("Listing saved with ID: {}", saved.id)
        return saved
    }

    private fun indexListingInElasticsearch(
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

    private fun buildListingsResponse(
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

    private val log = LoggerFactory.getLogger(ListingsService::class.java)
}
