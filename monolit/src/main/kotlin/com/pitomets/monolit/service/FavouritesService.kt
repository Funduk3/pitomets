package com.pitomets.monolit.service

import com.pitomets.monolit.exceptions.AlreadyException
import com.pitomets.monolit.exceptions.ListingNotFoundException
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.model.entity.Favourite
import com.pitomets.monolit.repository.FavouritesRepo
import com.pitomets.monolit.repository.ListingsRepo
import com.pitomets.monolit.repository.findListingOrThrow
import org.springframework.stereotype.Service

@Service
class FavouritesService(
    private val favouritesRepo: FavouritesRepo,
    private val listingsRepo: ListingsRepo,
) {
    fun getFavourites(
        userId: Long
    ): List<SearchListingsResponse> {
        val favoriteItemIds = favouritesRepo.findAllByUserId(userId).map { it.listingId }

        if (favoriteItemIds.isEmpty()) return emptyList()

        val listings = listingsRepo.findAllByIdIn(favoriteItemIds)

        return listings.map { listing ->
            SearchListingsResponse(
                id = requireNotNull(listing.id),
                title = listing.title,
                description = listing.description,
                price = listing.price,
                cityTitle = listing.city.title,
            )
        }
    }

    fun addFavourite(
        userId: Long,
        listingId: Long,
    ): SearchListingsResponse {
        val existing = favouritesRepo.findAllByUserId(userId)
            .any { it.listingId == listingId }

        if (existing) {
            throw AlreadyException("Item already in favorites")
        }

        val listing = listingsRepo.findListingOrThrow(listingId)

        favouritesRepo.save(
            Favourite(userId = userId, listingId = listingId)
        )

        return SearchListingsResponse(
            id = requireNotNull(listing.id),
            title = listing.title,
            description = listing.description,
            price = listing.price,
            cityTitle = listing.city.title,
        )
    }

    fun deleteFavourite(
        userId: Long,
        listingId: Long
    ) {
        val favourite = favouritesRepo.findByUserIdAndListingId(userId, listingId)
            ?: throw ListingNotFoundException("Favourite not found")
        favouritesRepo.delete(favourite)
    }
}
