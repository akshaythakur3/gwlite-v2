package com.gwlite.versioning;

import com.gwlite.model.Document;
import com.gwlite.model.DocumentVersion;
import com.gwlite.repository.DocumentRepository;
import com.gwlite.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fixes drawback #4: takes a version snapshot of every document that has
 * changed since its last snapshot, every 5 minutes, and uploads it to S3.
 * MySQL only stores the pointer (S3 key) + metadata, keeping row sizes small.
 */
@Component
@RequiredArgsConstructor
public class DocumentSnapshotScheduler {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final S3Service s3Service;

    private static final DateTimeFormatter KEY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    // Runs every 5 minutes (300,000 ms)
    @Scheduled(fixedRate = 300_000)
    public void snapshotAllDocuments() {
        List<Document> documents = documentRepository.findAll();

        for (Document doc : documents) {
            if (!hasChangedSinceLastSnapshot(doc)) {
                continue; // skip unchanged docs - no point paying S3 write costs for no-ops
            }

            String key = "documents/" + doc.getId() + "/versions/" +
                    LocalDateTime.now().format(KEY_TIMESTAMP) + ".txt";

            s3Service.uploadSnapshot(key, doc.getContent());

            DocumentVersion version = new DocumentVersion();
            version.setDocument(doc);
            version.setS3Key(key);
            version.setDocumentVersionNumber(doc.getVersion());
            documentVersionRepository.save(version);
        }
    }

    private boolean hasChangedSinceLastSnapshot(Document doc) {
        List<DocumentVersion> versions = documentVersionRepository.findByDocumentIdOrderBySnapshotAtDesc(doc.getId());
        if (versions.isEmpty()) return true; // never snapshotted
        return !versions.get(0).getDocumentVersionNumber().equals(doc.getVersion());
    }
}
