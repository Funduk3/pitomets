package com.pitomets.monolit.controller

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.model.dto.request.AddFavouriteRequest
import com.pitomets.monolit.model.dto.response.SearchListingsResponse
import com.pitomets.monolit.service.FavouritesService
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    ): List<SearchListingsResponse> {
        return favouritesService.getFavourites(userPrincipal.id)
    }

    @PostMapping("/favourites")
    fun addToFavourite(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: AddFavouriteRequest
    ): SearchListingsResponse {
        return favouritesService.addFavourite(userPrincipal.id, request.listingId)
    }
}
