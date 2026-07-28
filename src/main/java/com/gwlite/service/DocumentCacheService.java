package com.gwlite.service;

import com.gwlite.model.Document;
import com.gwlite.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Read-through / write-invalidate cache for document content.
 * Fixes drawback #2: without this, every document open hits MySQL directly.
 */
@Service
@RequiredArgsConstructor
public class DocumentCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DocumentRepository documentRepository;
    private static final long TTL_SECONDS = 300; // 5 minutes

    public String getContent(Long documentId) {
        String key = cacheKey(documentId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached; // cache HIT - no DB round trip
        }

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        redisTemplate.opsForValue().set(key, doc.getContent(), TTL_SECONDS, TimeUnit.SECONDS);
        return doc.getContent();
    }

    public void updateCacheOnWrite(Long documentId, String newContent) {
        redisTemplate.opsForValue().set(cacheKey(documentId), newContent, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void invalidate(Long documentId) {
        redisTemplate.delete(cacheKey(documentId));
    }

    private String cacheKey(Long documentId) {
        return "doc:" + documentId + ":content";
    }
}
