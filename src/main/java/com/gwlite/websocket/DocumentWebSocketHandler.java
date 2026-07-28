package com.gwlite.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwlite.dto.RedisEditMessage;
import com.gwlite.model.Document;
import com.gwlite.repository.DocumentRepository;
import com.gwlite.security.JwtUtil;
import com.gwlite.service.DocumentCacheService;
import com.gwlite.service.PresenceService;
import com.gwlite.monitoring.AppMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handles real-time collaborative editing.
 *
 * SCALING NOTE (fixes drawback #1): this handler no longer broadcasts only to
 * its own local session map. It PUBLISHES every edit to Redis, and every app
 * node (including this one) receives it via DocumentEditSubscriber and pushes
 * to its own locally-connected sessions. This means edits reach collaborators
 * regardless of which app instance they're connected to behind the load balancer.
 *
 * CACHING NOTE (fixes drawback #2): reads go through DocumentCacheService,
 * and presence is tracked via PresenceService (Redis, not MySQL - presence
 * is ephemeral data and doesn't belong in the relational store).
 */
@Component
@RequiredArgsConstructor
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    private final DocumentRepository documentRepository;
    private final DocumentCacheService documentCacheService;
    private final PresenceService presenceService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final AppMetrics appMetrics;

    // Unique per-JVM instance id - lets a node recognize (and optionally skip) its own echoes
    private final String nodeId = UUID.randomUUID().toString();

    // Tracks only THIS node's local sessions per document - cross-node fanout goes through Redis
    private final Map<Long, Set<WebSocketSession>> localSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToUserId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long documentId = extractDocumentId(session);
        localSessions.computeIfAbsent(documentId, id -> new CopyOnWriteArraySet<>()).add(session);
        appMetrics.recordWebSocketConnection();

        Long userId = extractUserId(session);
        if (userId != null) {
            sessionToUserId.put(session.getId(), userId);
            presenceService.markActive(documentId, userId);
            broadcastPresenceUpdate(documentId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long documentId = extractDocumentId(session);
        String newContent = message.getPayload();

        // Persist to DB
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc != null) {
            doc.setContent(newContent);
            doc.setUpdatedAt(LocalDateTime.now());
            doc.setVersion(doc.getVersion() + 1);
            documentRepository.save(doc);
        }

        // Update cache immediately (write-through, see DocumentCacheService)
        documentCacheService.updateCacheOnWrite(documentId, newContent);
        appMetrics.recordDocumentEdit();

        // Publish to Redis so every node (this one + others) can fan out to its sessions
        RedisEditMessage payload = new RedisEditMessage(documentId, newContent, nodeId);
        redisTemplate.convertAndSend("doc:" + documentId, objectMapper.writeValueAsString(payload));
    }

    /** Called by DocumentEditSubscriber when a Redis pub/sub message arrives. */
    public void broadcastLocally(Long documentId, String content, String originNodeId) {
        Set<WebSocketSession> sessions = localSessions.getOrDefault(documentId, Set.of());
        for (WebSocketSession peer : sessions) {
            if (peer.isOpen()) {
                try {
                    peer.sendMessage(new TextMessage(content));
                } catch (IOException e) {
                    System.err.println("Failed to send to session " + peer.getId());
                }
            }
        }
    }

    private void broadcastPresenceUpdate(Long documentId) {
        Set<String> viewers = presenceService.getActiveViewers(documentId);
        String presenceJson;
        try {
            presenceJson = objectMapper.writeValueAsString(Map.of("type", "presence", "viewers", viewers));
        } catch (Exception e) {
            return;
        }
        for (WebSocketSession peer : localSessions.getOrDefault(documentId, Set.of())) {
            if (peer.isOpen()) {
                try { peer.sendMessage(new TextMessage(presenceJson)); } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long documentId = extractDocumentId(session);
        Set<WebSocketSession> room = localSessions.get(documentId);
        if (room != null) {
            room.remove(session);
            if (room.isEmpty()) localSessions.remove(documentId);
        }

        Long userId = sessionToUserId.remove(session.getId());
        if (userId != null) {
            presenceService.markInactive(documentId, userId);
            broadcastPresenceUpdate(documentId);
        }
    }

    private Long extractDocumentId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        return Long.parseLong(idStr);
    }

    private Long extractUserId(WebSocketSession session) {
        // Client connects to /ws/document/{id}?token=JWT ; extract user from token
        String query = session.getUri().getQuery();
        if (query == null || !query.contains("token=")) return null;
        String token = query.substring(query.indexOf("token=") + 6);
        try {
            String email = jwtUtil.extractEmail(token);
            // In a full implementation, resolve email -> userId via UserRepository.
            // Kept simple here; wire in UserService.getByEmail(email).getId() in production.
            return (long) email.hashCode();
        } catch (Exception e) {
            return null;
        }
    }
}
