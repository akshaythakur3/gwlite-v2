package com.gwlite.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwlite.dto.RedisEditMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Listens for edit events published by ANY app node (including itself) and
 * relays them to whichever WebSocket sessions are connected locally to this
 * specific node. This is the piece that makes multi-instance scaling work -
 * without it, users on different nodes editing the same doc never see each
 * other's changes.
 */
@Component
@RequiredArgsConstructor
public class DocumentEditSubscriber implements MessageListener {

    private final DocumentWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            RedisEditMessage payload = objectMapper.readValue(message.getBody(), RedisEditMessage.class);
            webSocketHandler.broadcastLocally(payload.getDocumentId(), payload.getContent(), payload.getOriginNodeId());
        } catch (Exception e) {
            // A single malformed message shouldn't kill the listener thread
            System.err.println("Failed to process Redis edit message: " + e.getMessage());
        }
    }
}
