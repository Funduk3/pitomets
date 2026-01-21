package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.exceptions.PetNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.CityDto
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.dto.response.MetroDto
import com.pitomets.monolit.model.dto.response.MetroLineDto
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.entity.Pet
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.repository.CitiesRepository
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.MetroStationRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException

@Service
@Suppress("TooManyFunctions")
class ListingsService(
    private val petsRepo: PetsRepo,
    private val listingsRepo: ListingsRepo,
    private val sellerProfileRepo: SellerProfileRepo,
    private val outboxRepo: ListingOutboxRepository,
    private val cityRepo: CitiesRepository,
    private val metroRepo: MetroStationRepo,
) {
    // Controller functions

    @Transactional
    fun createListing(
        userId: Long,
        request: ListingsRequest
    ): ListingsResponse {
        val seller = findSellerProfile(userId, sellerProfileRepo, log)
        val (father, mother) = findParentPets(request, petsRepo, log)

        val listing = createListingEntity(
            request,
            seller,
            father,
            mother,
            request.cityId,
            request.metroId
        )
        val saved = listingsRepo.save(listing)

        outboxRepo.save(
            ListingOutbox(
                listingId = requireNotNull(saved.id),
                eventType = EventType.CREATE,
                title = saved.title,
                description = saved.description
            )
        )

        return buildListingsResponse(saved, father, mother)
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
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun updateListing(
        listingId: Long,
        sellerId: Long,
        request: UpdateListingRequest
    ): ListingsResponse {
        val listing = requireOwnerAndReturnListing(listingId, sellerId)

        request.title?.let { listing.title = it }
        request.description?.let { listing.description = it }
        request.species?.let { listing.species = it }
        request.price?.let { listing.price = it }
        request.ageMonths?.let { listing.ageMonths = it }
        request.breed?.let { listing.breed = it }
        request.isArchived?.let { listing.isArchived = it }

        request.mother?.let {
            listing.mother = petsRepo.findById(it)
                .orElseThrow { PetNotFoundException("Mother with id $it") }
        }

        request.father?.let {
            listing.father = petsRepo.findById(it)
                .orElseThrow { PetNotFoundException("Father with id $it") }
        }

        request.city?.let {
            listing.city = cityRepo.findById(it).orElseThrow()
        }

        request.metroStation?.let {
            listing.metroStation = metroRepo.findById(it).orElseThrow()
        }

        val saved = listingsRepo.save(listing)

        val shouldIndex = request.title != null || request.description != null

        if (shouldIndex) {
            outboxRepo.save(
                ListingOutbox(
                    listingId = listingId,
                    eventType = EventType.UPDATE,
                    title = saved.title,
                    description = saved.description
                )
            )
        }

        return ListingsResponse(
            listingsId = listingId,
            title = saved.title,
            description = saved.description,
            species = saved.species,
            ageMonths = saved.ageMonths,
            price = saved.price,
            breed = saved.breed,
            isArchived = saved.isArchived,
            sellerId = requireNotNull(saved.sellerProfile.seller?.id),
            sellerRating = saved.sellerProfile.rating,
            sellerReviewsCount = saved.sellerProfile.countReviews,
            mother = saved.mother?.id,
            father = saved.father?.id,
            city = CityDto(
                id = saved.city.id,
                title = saved.city.title
            ),
            metro = saved.metroStation?.let { station ->
                MetroDto(
                    id = station.id,
                    title = station.title,
                    line = MetroLineDto(
                        id = station.line.id,
                        title = station.line.title,
                        color = station.line.color
                    )
                )
            }
        )
    }

    @Transactional
    fun deleteListing(
        listingId: Long,
        userId: Long
    ) {
        val listing = requireOwnerAndReturnListing(listingId, userId)

        listingsRepo.delete(listing)

        outboxRepo.save(
            ListingOutbox(
                listingId = listingId,
                eventType = EventType.DELETE,
                title = null,
                description = null
            )
        )
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

    fun ListingsRepo.findListingOrThrow(listingId: Long): Listing =
        findByIdOrNull(listingId)
            ?: throw ListingNotFoundException("Listing with id $listingId not found")

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
        mother: Pet?,
        cityId: Long,
        metroId: Long?,
    ) = Listing(
        description = request.description,
        species = request.species,
        breed = request.breed,
        ageMonths = request.ageMonths,
        father = father,
        mother = mother,
        price = request.price,
        sellerProfile = seller,
        title = request.title,
        city = cityRepo.findById(cityId)
            .orElseThrow(),
        metroStation = metroId?.let {
            metroRepo.findById(it)
                .orElseThrow()
        }
    )

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
        title = savedListing.title,
        city = CityDto(
            id = savedListing.city.id,
            title = savedListing.city.title
        ),
        metro = savedListing.metroStation?.let { station ->
            MetroDto(
                id = station.id,
                title = station.title,
                line = MetroLineDto(
                    id = station.line.id,
                    title = station.line.title,
                    color = station.line.color
                )
            )
        }
    )

    private val log = LoggerFactory.getLogger(ListingsService::class.java)
}
