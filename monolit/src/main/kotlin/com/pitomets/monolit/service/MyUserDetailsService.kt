package com.pitomets.monolit.service

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.repository.UserRepo
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class MyUserDetailsService(
    private val userRepo: UserRepo
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepo.findByFullName(username)
        if (user == null) {
            println("User Not Found")
            throw UsernameNotFoundException("user not found")
        }

        return UserPrincipal(user)
    }
}
