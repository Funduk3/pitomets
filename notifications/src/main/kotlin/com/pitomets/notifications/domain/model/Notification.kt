package com.pitomets.notifications.domain.model

import com.pitomets.notifications.application.command.SendNotificationCommand

data class Notification(
    val id: Long?,
    val eventId: Long,
    val userId: Long,
    val channel: Channel,
    val messageType: MessageType,
    val payload: String,
    val status: Status
) {

    fun markSent() = copy(status = Status.SENT)
    fun markFailed() = copy(status = Status.FAILED)

    companion object {
        fun create(cmd: SendNotificationCommand) =
            Notification(
                id = null,
                eventId = cmd.eventId,
                userId = cmd.userId,
                channel = cmd.channel,
                messageType = cmd.messageType,
                payload = cmd.payload,
                status = Status.NEW
            )
    }
}
