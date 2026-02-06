package com.pitomets.notifications.exceptions

class FailedToSendNotificationException : RuntimeException {
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
