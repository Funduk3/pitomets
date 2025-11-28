package com.pitomets.monolit.model

import com.pitomets.monolit.model.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(private val user: User) : UserDetails {

    val id: Long
        get() = requireNotNull(user.id) { "User ID cannot be null" }

    val email: String
        get() = requireNotNull(user.email) { "User email cannot be null" }

    val isSeller: Boolean
        get() = user.sellerProfile != null

    val isBuyer: Boolean
        get() = user.buyerProfile != null

    override fun getUsername(): String = user.email!!

    override fun getPassword(): String = user.passwordHash

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        if (isBuyer) authorities.add(SimpleGrantedAuthority("ROLE_BUYER"))
        if (isSeller) authorities.add(SimpleGrantedAuthority("ROLE_SELLER"))
        return authorities
    }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean {
        return user.bannedUntil == null || user.bannedUntil!!.isBefore(java.time.OffsetDateTime.now())
    }

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
