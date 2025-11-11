package com.pitomets.monolit.model

import com.pitomets.monolit.model.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

// класс для авторизации
class UserPrincipal(
    private val user: User
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority?> {
        return setOf(SimpleGrantedAuthority("USER"))
    }

    override fun getPassword(): String {
        return user.password
    }

    override fun getUsername(): String {
        return user.name
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}