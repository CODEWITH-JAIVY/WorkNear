package com.labourse.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labourse.chat.dto.IncomingMessage;
import com.labourse.chat.entity.ChatMessage;
import com.labourse.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatRoomManager roomManager;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Connection URL: /ws/chat?jobId=123&userId=45 — gateway has already validated the JWT
    // before this request reached here (chat routes go through the same JwtAuthGatewayFilter).
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        roomManager.join(jobId(session), userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomManager.leave(jobId(session), userId(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        IncomingMessage incoming = objectMapper.readValue(message.getPayload(), IncomingMessage.class);
        Long senderId = Long.valueOf(userId(session));

        ChatMessage saved = new ChatMessage();
        saved.setJobId(incoming.getJobId());
        saved.setSenderId(senderId);
        saved.setReceiverId(incoming.getReceiverId());
        saved.setContent(incoming.getContent());

        // Deliver first (best-effort real-time), persist regardless — history must survive
        // even if the other participant is offline right now.
        Map<String, WebSocketSession> room = roomManager.participantsOf(incoming.getJobId());
        WebSocketSession receiverSession = room.get(String.valueOf(incoming.getReceiverId()));
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(saved)));
            saved.setDelivered(true);
        }

        messageRepository.save(saved);
    }

    private Long jobId(WebSocketSession session) {
        return Long.valueOf(queryParam(session, "jobId"));
    }

    private String userId(WebSocketSession session) {
        return queryParam(session, "userId");
    }

    private String queryParam(WebSocketSession session, String key) {
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith(key + "=")) return param.substring(key.length() + 1);
        }
        return null;
    }
}
