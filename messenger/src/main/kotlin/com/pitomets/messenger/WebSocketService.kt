package src.main.kotlin.com.pitomets.messenger

import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import io.ktor.websocket.incoming
import io.ktor.websocket.Frame
import java.awt.Frame

class WebSocketService {
    suspend fun handleChatSession(session: WebSocketSession) {
        session.send("Hello from server!")
        for (frame in session.incoming) {
            frame as? Frame.Text ?: continue
            val message = frame.readText()
            session.send("Echo: $message")
        }
    }
}
