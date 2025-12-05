package com.pitomets.monolit.repository

import com.pitomets.monolit.model.entity.BuyerProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BuyerProfileRepo : JpaRepository<BuyerProfile, Long>
