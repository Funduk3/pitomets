package com.pitomets.notifications.domain.model

import com.pitomets.notifications.application.command.SendNotificationCommand
import kotlin.random.Random

data class Notification(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val payload: String,
    val status: Status
) {

    fun markSent() = copy(status = Status.SENT)

    companion object {
        fun create(cmd: SendNotificationCommand) =
            Notification(
                id = Random.nextLong(),
                eventId = cmd.eventId,
                userId = cmd.userId,
                channel = cmd.channel,
                payload = cmd.payload,
                status = Status.NEW
            )
    }
}

