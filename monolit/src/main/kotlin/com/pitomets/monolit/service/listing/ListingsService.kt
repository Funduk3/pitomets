package com.pitomets.monolit.service.listing

import com.pitomets.monolit.exceptions.BadAgeException
import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.exceptions.PetNotFoundException
import com.pitomets.monolit.exceptions.UserNotFoundException
import com.pitomets.monolit.model.AgeEnum
import com.pitomets.monolit.model.EventType
import com.pitomets.monolit.model.dto.request.ListingsRequest
import com.pitomets.monolit.model.dto.request.UpdateListingRequest
import com.pitomets.monolit.model.dto.response.CityDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.kafka.moderation.ModerationOperation
import com.pitomets.monolit.model.dto.response.ListingsCursorResponse
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.model.dto.response.MetroDto
import com.pitomets.monolit.model.dto.response.MetroLineDto
import com.pitomets.monolit.model.dto.response.PhotoModerationHintResponse
import com.pitomets.monolit.model.kafka.moderation.ModerationEntityType
import com.pitomets.monolit.model.entity.Listing
import com.pitomets.monolit.model.entity.ListingOutbox
import com.pitomets.monolit.model.entity.Pet
import com.pitomets.monolit.model.entity.SellerProfile
import com.pitomets.monolit.model.entity.AiTextModerationReport
import com.pitomets.monolit.model.entity.AiPhotoModerationReport
import com.pitomets.monolit.repository.CitiesRepository
import com.pitomets.monolit.repository.AiTextReportRepo
import com.pitomets.monolit.repository.ListingOutboxRepository
import com.pitomets.monolit.repository.AiPhotoReportRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.MetroStationRepo
import com.pitomets.monolit.repository.PetsRepo
import com.pitomets.monolit.repository.SellerProfileRepo
import com.pitomets.monolit.repository.ListingPhotoRepo
import com.pitomets.monolit.service.moderation.ModerationRequestService
import com.pitomets.monolit.service.moderationHint
import com.pitomets.monolit.service.PhotoModerationUrlService
import com.pitomets.monolit.service.PhotoUrlService
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
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
    private val redisTemplate: RedisTemplate<String, String>,
    private val metricsService: ListingMetricsService,
    private val moderationRequestService: ModerationRequestService,
    private val aiTextReportRepo: AiTextReportRepo,
    private val aiPhotoReportRepo: AiPhotoReportRepo,
    private val listingPhotoRepo: ListingPhotoRepo,
    private val photoUrlService: PhotoUrlService,
    private val photoModerationUrlService: PhotoModerationUrlService,
) {
    @Transactional
    fun createListing(
        userId: Long,
        request: ListingsRequest
    ): ListingsResponse {
        if (request.ageMonths < 0 || request.ageMonths >= AgeEnum.entries.size) {
            throw BadAgeException("Invalid ageMonths: ${request.ageMonths}")
        }

        val seller = findSellerProfile(userId, sellerProfileRepo, log)
        if (!seller.isApproved) {
            throw AccessDeniedException("Seller profile is pending approval")
        }
        val (father, mother) = findParentPets(request, petsRepo, log)

        val listing = createListingEntity(
            request,
            seller,
            father,
            mother,
            request.cityId,
            request.metroId,
        )
        val saved = listingsRepo.save(listing)
        moderationRequestService.publishListing(saved, ModerationOperation.CREATE)

        if (saved.isApproved) {
            outboxRepo.save(
                ListingOutbox(
                    listingId = requireNotNull(saved.id),
                    eventType = EventType.CREATE,
                    title = saved.title,
                    description = saved.description,
                    species = saved.species,
                    breed = saved.breed,
                    gender = saved.gender,
                    ageEnum = AgeEnum.entries.getOrNull(saved.ageMonths)?.name,
                    cityTitle = saved.city.title,
                    city = saved.city.id,
                    metro = saved.metroStation?.id,
                    price = saved.price,
                )
            )
        }

        return buildListingsResponse(saved, father, mother)
    }

    fun getListing(
        listingId: Long,
        currentUserId: Long? = null
    ): ListingsResponse {
        val response = listingsRepo.findListingOrThrow(listingId)
        val isOwner = response.sellerProfile.seller?.id == currentUserId
        if (!response.isApproved && !isOwner) {
            throw ListingNotFoundException("Listing with id $listingId not found")
        }
        return buildListingsResponse(
            response,
            response.father,
            response.mother
        )
    }

    fun getListingEntity(listingId: Long): Listing =
        listingsRepo.findListingOrThrow(listingId)

    fun getListingWithView(
        listingId: Long,
        viewerId: Long?,
        viewerIp: String?,
        userAgent: String?
    ): ListingsResponse {
        val response = listingsRepo.findListingOrThrow(listingId)
        val isOwner = response.sellerProfile.seller?.id == viewerId
        if (!response.isApproved && !isOwner) {
            throw ListingNotFoundException("Listing with id $listingId not found")
        }

        metricsService.recordViewAsync(
            listingId = listingId,
            ownerId = response.sellerProfile.seller?.id,
            viewerId = viewerId,
            ip = viewerIp,
            userAgent = userAgent
        )

        val views = response.viewsCount + metricsService.getPendingViewsDelta(listingId)
        val likes = response.likesCount + metricsService.getPendingLikesDelta(listingId)

        return buildListingsResponse(
            response,
            response.father,
            response.mother,
            viewsCount = views,
            likesCount = likes
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

    fun getSellerListingsPublic(sellerId: Long): List<ListingsResponse> {
        val seller = findSellerProfile(sellerId, sellerProfileRepo, log)
        val listings = listingsRepo.findBySellerProfileAndIsApprovedTrue(seller)
        return listings.map { listing ->
            buildListingsResponse(
                listing,
                listing.father,
                listing.mother
            )
        }
    }

    fun getHomeListings(cursor: Long?): ListingsCursorResponse {
        val cacheKey = homeCacheKey(cursor)
        redisTemplate.opsForValue().get(cacheKey)?.let { cached ->
            return mapper.readValue(cached, ListingsCursorResponse::class.java)
        }

        val pageable = PageRequest.of(
            0,
            HOME_PAGE_SIZE + 1,
            Sort.by(Sort.Direction.DESC, "id")
        )

        val result = if (cursor == null) {
            listingsRepo.findByIsArchivedFalseAndIsApprovedTrueOrderByIdDesc(pageable)
        } else {
            listingsRepo.findByIsArchivedFalseAndIsApprovedTrueAndIdLessThanOrderByIdDesc(cursor, pageable)
        }

        val slice = result.take(HOME_PAGE_SIZE)
        val hasMore = result.size > HOME_PAGE_SIZE
        val nextCursor = slice.lastOrNull()?.id?.takeIf { hasMore }

        val response = ListingsCursorResponse(
            items = slice.map { listing ->
                buildListingsResponse(
                    listing,
                    listing.father,
                    listing.mother
                )
            },
            nextCursor = nextCursor,
            hasMore = hasMore
        )

        redisTemplate.opsForValue().set(cacheKey, mapper.writeValueAsString(response), CACHE_TTL)
        return response
    }

    @Transactional
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun updateListing(
        listingId: Long,
        sellerId: Long,
        request: UpdateListingRequest
    ): ListingsResponse {
        val listing = requireOwnerAndReturnListing(listingId, sellerId)
        val wasApproved = listing.isApproved

        request.title?.let { listing.title = it }
        request.description?.let { listing.description = it }
        request.species?.let { listing.species = it }
        request.price?.let { listing.price = it }
        request.ageMonths?.let { listing.ageMonths = it }
        request.gender?.let { listing.gender = it }
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

        listing.isApproved = false
        listing.manualModerationPending = true
        clearListingModeratorMessage(listingId)

        val saved = listingsRepo.save(listing)
        moderationRequestService.publishListing(saved, ModerationOperation.UPDATE)

        if (wasApproved) {
            outboxRepo.save(
                ListingOutbox(
                    listingId = listingId,
                    eventType = EventType.DELETE,
                    title = null,
                    description = null,
                    city = 0,
                    metro = null,
                    price = 0.toBigDecimal(),
                )
            )
        }

        val report = getListingReport(listingId)
        val photoHint = getListingPhotoHint(listingId)
        return ListingsResponse(
            listingsId = listingId,
            title = saved.title,
            description = saved.description,
            species = saved.species,
            ageMonths = saved.ageMonths,
            gender = saved.gender,
            price = saved.price,
            breed = saved.breed,
            isArchived = saved.isArchived,
            sellerId = requireNotNull(saved.sellerProfile.seller?.id),
            sellerRating = saved.sellerProfile.rating,
            sellerReviewsCount = saved.sellerProfile.countReviews,
            coverPhotoId = saved.coverPhotoId,
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
            },
            viewsCount = saved.viewsCount,
            likesCount = saved.likesCount,
            isApproved = saved.isApproved,
            manualModerationPending = saved.manualModerationPending,
            moderatorMessage = report?.moderatorMessage,
            photoModerationHint = photoHint
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
                description = null,
                city = 0,
                metro = null,
                price = 0.toBigDecimal(),
            )
        )
    }

    @Transactional
    fun approveListing(id: Long) {
        val listing = listingsRepo.findListingOrThrow(id)
        listing.isApproved = true
        listing.manualModerationPending = false
        clearListingModeratorMessage(id)
        val saved = listingsRepo.save(listing)

        outboxRepo.save(
            ListingOutbox(
                listingId = requireNotNull(saved.id),
                eventType = EventType.UPDATE,
                title = saved.title,
                description = saved.description,
                species = saved.species,
                breed = saved.breed,
                gender = saved.gender,
                ageEnum = AgeEnum.entries.getOrNull(saved.ageMonths)?.name,
                cityTitle = saved.city.title,
                city = saved.city.id,
                metro = saved.metroStation?.id,
                price = saved.price,
            )
        )
    }

    @Transactional
    fun declineListing(id: Long, message: AdminMessage) {
        val listing = listingsRepo.findListingOrThrow(id)
        listing.isApproved = false
        listing.manualModerationPending = false
        val report = getListingReport(id)
        val declineMessage = buildDeclineMessage(
            moderatorMessage = message.message,
            profanityDetected = report?.aiProfanityDetected == true,
            sexualContentDetected = report?.aiSexualContentDetected == true
        )
        setListingModeratorMessage(id, declineMessage)
        listingsRepo.save(listing)

        outboxRepo.save(
            ListingOutbox(
                listingId = requireNotNull(listing.id),
                eventType = EventType.DELETE,
                title = null,
                description = null,
                city = 0,
                metro = null,
                price = 0.toBigDecimal(),
            )
        )
    }

    fun getPendingListings(): List<ListingsResponse> {
        return listingsRepo.findByManualModerationPendingTrue().map { listing ->
            buildListingsResponse(
                listing,
                listing.father,
                listing.mother,
                includeModerationHint = true
            )
        }
    }

    fun getPendingListing(listingId: Long): ListingsResponse {
        val listing = listingsRepo.findByIdAndManualModerationPendingTrue(listingId)
            ?: throw ListingNotFoundException("Pending listing with id $listingId not found")
        return buildListingsResponse(
            listing,
            listing.father,
            listing.mother,
            includeModerationHint = true
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
        gender = request.gender,
        father = father,
        mother = mother,
        price = request.price,
        sellerProfile = seller,
        title = request.title,
        isApproved = false,
        manualModerationPending = true,
        city = cityRepo.findById(cityId)
            .orElseThrow(),
        metroStation = metroId?.let {
            metroRepo.findById(it)
                .orElseThrow()
        },
    )

    private fun buildListingsResponse(
        savedListing: Listing,
        father: Pet?,
        mother: Pet?,
        viewsCount: Long = savedListing.viewsCount,
        likesCount: Long = savedListing.likesCount,
        includeModerationHint: Boolean = false
    ): ListingsResponse {
        val listingId = requireNotNull(savedListing.id)
        val report = getListingReport(listingId)
        val photoHint = getListingPhotoHint(listingId)
        return ListingsResponse(
        description = savedListing.description,
        sellerId = requireNotNull(savedListing.sellerProfile.seller?.id),
        sellerRating = savedListing.sellerProfile.rating,
        sellerReviewsCount = savedListing.sellerProfile.countReviews,
        coverPhotoId = savedListing.coverPhotoId,
        species = savedListing.species,
        breed = savedListing.breed,
        ageMonths = savedListing.ageMonths,
        gender = savedListing.gender,
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
        },
        viewsCount = viewsCount,
        likesCount = likesCount,
        isApproved = savedListing.isApproved,
        manualModerationPending = savedListing.manualModerationPending,
        moderatorMessage = report?.moderatorMessage,
        moderationHint = if (includeModerationHint) {
            toModerationHint(report)
        } else {
            null
        },
        photoModerationHint = if (includeModerationHint) photoHint else null
    )
    }

    private val log = LoggerFactory.getLogger(ListingsService::class.java)
    private val mapper = jacksonObjectMapper()

    private fun homeCacheKey(cursor: Long?): String =
        if (cursor == null) {
            "home:listings:cursor:none:size:$HOME_PAGE_SIZE"
        } else {
            "home:listings:cursor:$cursor:size:$HOME_PAGE_SIZE"
        }

    companion object {
        private const val HOME_PAGE_SIZE = 10
        private val CACHE_TTL = Duration.ofSeconds(30)
        private const val PRIORITY_REJECTED = 4
        private const val PRIORITY_REVIEW = 3
        private const val PRIORITY_ERROR = 2
        private const val PRIORITY_APPROVED = 1
    }

    private fun buildDeclineMessage(
        moderatorMessage: String,
        profanityDetected: Boolean,
        sexualContentDetected: Boolean
    ): String {
        val base = moderatorMessage.trim()
        val recommendations = mutableListOf<String>()

        if (profanityDetected) {
            recommendations += "уберите нецензурные выражения"
        }
        if (sexualContentDetected) {
            recommendations += "уберите контент 18+"
        }

        if (recommendations.isEmpty()) {
            return base
        }

        return "$base. Рекомендации: ${recommendations.joinToString("; ")}."
    }

    private fun getListingReport(listingId: Long): AiTextModerationReport? =
        aiTextReportRepo.findByEntityIdAndEntityType(listingId, ModerationEntityType.LISTING.name)

    private fun toModerationHint(report: AiTextModerationReport?) =
        moderationHint(
            status = report?.aiModerationStatus,
            reason = report?.aiModerationReason,
            toxicityScore = report?.aiToxicityScore,
            profanityDetected = report?.aiProfanityDetected,
            sexualContentDetected = report?.aiSexualContentDetected,
            sourceAction = report?.aiSourceAction,
            modelVersion = null
        )

    @Suppress("ReturnCount")
    private fun getListingPhotoHint(listingId: Long): PhotoModerationHintResponse? {
        val photos = listingPhotoRepo.findByListingIdOrderByPosition(listingId)
        val photoUrls = photos.map { photoUrlService.objectUrl(it.objectKey) }
        val moderationUrls = photos.map { photoModerationUrlService.objectUrl(it.objectKey) }
        val photoKeys = photos.map { it.objectKey }

        if (photoUrls.isEmpty()) {
            return null
        }

        val reports = aiPhotoReportRepo.findByPhotoUriInAndEntityIdAndEntityType(
            (photoUrls + moderationUrls + photoKeys).distinct(),
            listingId,
            ModerationEntityType.LISTING.name
        )
        val enriched = if (reports.isNotEmpty()) {
            reports
        } else {
            aiPhotoReportRepo.findByEntityIdAndEntityType(listingId, ModerationEntityType.LISTING.name)
        }

        val worst = pickWorstPhotoReport(enriched) ?: return null
        return toPhotoHint(worst)
    }

    private fun pickWorstPhotoReport(reports: List<AiPhotoModerationReport>): AiPhotoModerationReport? {
        if (reports.isEmpty()) {
            return null
        }

        val priority = mapOf(
            "REJECTED" to PRIORITY_REJECTED,
            "REVIEW" to PRIORITY_REVIEW,
            "ERROR" to PRIORITY_ERROR,
            "APPROVED" to PRIORITY_APPROVED
        )

        return reports.maxByOrNull { report ->
            priority[report.aiModerationStatus] ?: 0
        }
    }

    private fun toPhotoHint(report: AiPhotoModerationReport) = PhotoModerationHintResponse(
        status = report.aiModerationStatus,
        reason = report.aiModerationReason,
        toxicityScore = report.aiToxicityScore,
        labels = report.aiLabels,
        toxicTextDetected = report.aiToxicTextDetected,
        toxicTextMatches = report.aiToxicTextMatches,
        modelVersion = null
    )

    private fun clearListingModeratorMessage(listingId: Long) {
        val report = getListingReport(listingId) ?: return
        report.moderatorMessage = null
        aiTextReportRepo.save(report)
    }

    private fun setListingModeratorMessage(listingId: Long, message: String) {
        val report = getListingReport(listingId) ?: AiTextModerationReport(
            entityId = listingId,
            entityType = ModerationEntityType.LISTING.name
        )
        report.moderatorMessage = message
        aiTextReportRepo.save(report)
    }
}
