package com.pitomets.monolit.controller

import com.pitomets.monolit.model.dto.request.AdminMessage
import com.pitomets.monolit.model.dto.response.ListingsResponse
import com.pitomets.monolit.service.listing.ListingsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/listing")
class ModeratorListingsController(
    private val listingsService: ListingsService
) {
    @GetMapping("/all")
    fun getAllPendingListings(): List<ListingsResponse> =
        listingsService.getPendingListings()


    @GetMapping("/{id}")
    fun getPendingListingById(@PathVariable id: Long) =
        listingsService.getPendingListing(id)

    @PostMapping("/{id}/approve")
    fun acceptListing(@PathVariable id: Long) {
        listingsService.approveListing(id)
    }

    @PostMapping("/{id}/decline")
    fun declineListing(
        @PathVariable id: Long,
        @RequestBody adminMessage: AdminMessage
    ) {
        listingsService.declineListing(id, adminMessage)
    }
}
