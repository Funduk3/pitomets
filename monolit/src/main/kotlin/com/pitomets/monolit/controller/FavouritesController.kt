package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.FavouriteRequest
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.service.FavouritesService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FavouritesController(
    private val favouritesService: FavouritesService,
) {
    @GetMapping("/favourites")
    fun updateSellerProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): List<SearchListingsResponse> =
        favouritesService.getFavourites(userPrincipal.id)

    @PostMapping("/favourites")
    fun addToFavourite(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: FavouriteRequest
    ): SearchListingsResponse =
        favouritesService.addFavourite(userPrincipal.id, request.listingId)

    @DeleteMapping("/favourites")
    fun deleteFavourite(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: FavouriteRequest
    ) =
        favouritesService.deleteFavourite(userPrincipal.id, request.listingId)
}
