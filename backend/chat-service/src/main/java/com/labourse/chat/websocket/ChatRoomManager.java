package com.labourse.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// In-memory, single-instance only — same caveat as notification-service's SessionManager.
// Move to a Redis-backed pub/sub registry before running >1 replica of this service.
@Component
public class ChatRoomManager {

    // jobId -> (userId -> session)
    private final Map<Long, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void join(Long jobId, String userId, WebSocketSession session) {
        rooms.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(userId, session);
    }

    public void leave(Long jobId, String userId) {
        Map<String, WebSocketSession> room = rooms.get(jobId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) rooms.remove(jobId);
        }
    }

    public Map<String, WebSocketSession> participantsOf(Long jobId) {
        return rooms.getOrDefault(jobId, Map.of());
    }
}
