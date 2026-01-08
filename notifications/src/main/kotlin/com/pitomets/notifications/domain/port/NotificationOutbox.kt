package com.pitomets.notifications.domain.port

import org.springframework.stereotype.Repository

@Repository
interface NotificationOutbox {
    fun save(event: Any)
}
