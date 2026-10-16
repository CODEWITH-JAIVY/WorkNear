package com.labourse.notification.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
    }

    public void remove(String userId) {
        userSessions.remove(userId);
    }

    public WebSocketSession get(String userId) {
        return userSessions.get(userId);
    }
}
