package com.pitomets.monolit.service

import com.pitomets.monolit.model.UserPrincipal
import com.pitomets.monolit.repository.UserRepo
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class MyUserDetailsService(
    private val userRepo: UserRepo
) : UserDetailsService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepo.findByFullName(username)
        if (user == null) {
            log.error("User Not Found")
            throw UsernameNotFoundException("User not found")
        }
        return UserPrincipal(user)
    }
}
