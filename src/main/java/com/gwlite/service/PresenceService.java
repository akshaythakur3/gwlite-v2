package com.gwlite.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Tracks "who's currently viewing/editing this document" using Redis only.
 * This data is intentionally NOT stored in MySQL - it's ephemeral and
 * self-expiring, which is exactly what Redis TTL keys are for.
 *
 * Trade-off worth knowing: with a 30s heartbeat TTL, an ungracefully closed
 * tab (e.g. browser crash) can show as "online" for up to 30s after they've
 * actually left. Acceptable for presence UI; not acceptable if this were
 * being used for anything requiring precise real-time state.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long PRESENCE_TTL_SECONDS = 30;

    public void markActive(Long documentId, Long userId) {
        redisTemplate.opsForSet().add(presenceSetKey(documentId), userId.toString());
        redisTemplate.opsForValue().set(heartbeatKey(documentId, userId), "active", PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void markInactive(Long documentId, Long userId) {
        redisTemplate.opsForSet().remove(presenceSetKey(documentId), userId.toString());
        redisTemplate.delete(heartbeatKey(documentId, userId));
    }

    public Set<String> getActiveViewers(Long documentId) {
        Set<String> userIds = redisTemplate.opsForSet().members(presenceSetKey(documentId));
        if (userIds == null) return Set.of();

        // Filter out stale entries whose heartbeat TTL already expired
        return userIds.stream()
                .filter(id -> Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey(documentId, Long.parseLong(id)))))
                .collect(Collectors.toSet());
    }

    private String presenceSetKey(Long documentId) {
        return "doc:" + documentId + ":presence";
    }

    private String heartbeatKey(Long documentId, Long userId) {
        return "user:" + userId + ":presence:" + documentId;
    }
}
