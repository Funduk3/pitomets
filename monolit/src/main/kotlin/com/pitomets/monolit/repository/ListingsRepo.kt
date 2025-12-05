package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.Listing
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ListingsRepo : JpaRepository<Listing, Long>
