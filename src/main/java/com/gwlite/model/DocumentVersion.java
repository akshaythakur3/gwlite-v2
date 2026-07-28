package com.gwlite.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Metadata row for a point-in-time snapshot of a document. The actual
 * content blob lives in S3 (cheap, durable, infinitely scalable object
 * storage) - only the S3 key and a few fields live in MySQL. Storing large
 * text blobs directly in a relational table doesn't scale well for
 * versioning history; this keeps MySQL rows small and fast to query.
 */
@Entity
@Table(name = "document_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private String s3Key; // e.g. "documents/42/versions/2026-07-27T10-05-00.txt"

    @Column(nullable = false)
    private Long documentVersionNumber; // matches Document.version at snapshot time

    @Column(nullable = false, updatable = false)
    private LocalDateTime snapshotAt = LocalDateTime.now();
}
