package com.labourse.notification.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final SessionManager sessionManager;

    public void pushToUser(String userId, String payloadJson) {
        WebSocketSession session = sessionManager.get(userId);
        if (session == null || !session.isOpen()) {
            log.info("User {} not connected — falling back to push notification (FCM) here", userId);
            // TODO: FCM fallback for mobile when the WebSocket isn't open
            return;
        }
        try {
            session.sendMessage(new TextMessage(payloadJson));
        } catch (IOException e) {
            log.error("Failed to push to user {}", userId, e);
        }
    }
}
