package com.gwlite.controller;

import com.gwlite.model.DocumentVersion;
import com.gwlite.repository.DocumentVersionRepository;
import com.gwlite.versioning.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents/{documentId}/versions")
@RequiredArgsConstructor
public class VersionController {

    private final DocumentVersionRepository documentVersionRepository;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<List<DocumentVersion>> listVersions(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentVersionRepository.findByDocumentIdOrderBySnapshotAtDesc(documentId));
    }

    @GetMapping("/{versionId}/content")
    public ResponseEntity<Map<String, String>> getVersionContent(@PathVariable Long documentId,
                                                                   @PathVariable Long versionId) {
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));
        String content = s3Service.downloadSnapshot(version.getS3Key());
        return ResponseEntity.ok(Map.of("content", content));
    }
}
