package com.gwlite.service;

import com.gwlite.dto.DocumentRequest;
import com.gwlite.dto.ShareRequest;
import com.gwlite.model.*;
import com.gwlite.repository.DocumentRepository;
import com.gwlite.repository.FolderRepository;
import com.gwlite.repository.SharePermissionRepository;
import com.gwlite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final SharePermissionRepository sharePermissionRepository;
    private final UserRepository userRepository;
    private final DocumentCacheService documentCacheService;

    public Document createDocument(DocumentRequest request, User owner) {
        Document doc = new Document();
        doc.setTitle(request.getTitle());
        doc.setContent(request.getContent() != null ? request.getContent() : "");
        doc.setOwner(owner);

        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
            doc.setFolder(folder);
        }

        return documentRepository.save(doc);
    }

    public Document getDocument(Long documentId, User requester) {
        Document doc = findDocumentOrThrow(documentId);
        requireAtLeastViewer(doc, requester);
        // Content served through cache (drawback #2 fix) - falls back to DB on miss
        doc.setContent(documentCacheService.getContent(documentId));
        return doc;
    }

    public Document updateDocument(Long documentId, DocumentRequest request, User requester) {
        Document doc = findDocumentOrThrow(documentId);
        requireAtLeastEditor(doc, requester);

        if (request.getTitle() != null) doc.setTitle(request.getTitle());
        if (request.getContent() != null) doc.setContent(request.getContent());
        doc.setUpdatedAt(LocalDateTime.now());
        doc.setVersion(doc.getVersion() + 1);

        Document saved = documentRepository.save(doc);
        documentCacheService.updateCacheOnWrite(documentId, saved.getContent());
        return saved;
    }

    public void deleteDocument(Long documentId, User requester) {
        Document doc = findDocumentOrThrow(documentId);
        if (!doc.getOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Only the owner can delete this document");
        }
        documentRepository.delete(doc);
    }

    public List<Document> getMyDocuments(User owner) {
        return documentRepository.findByOwner(owner);
    }

    public List<Document> getSharedWithMe(User user) {
        return sharePermissionRepository.findByUser(user).stream()
                .map(SharePermission::getDocument)
                .collect(Collectors.toList());
    }

    public SharePermission shareDocument(Long documentId, ShareRequest request, User requester) {
        Document doc = findDocumentOrThrow(documentId);

        if (!doc.getOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Only the owner can share this document");
        }

        User targetUser = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User with that email not found"));

        SharePermission permission = sharePermissionRepository
                .findByDocumentIdAndUserId(documentId, targetUser.getId())
                .orElse(new SharePermission());

        permission.setDocument(doc);
        permission.setUser(targetUser);
        permission.setRole(request.getRole());

        return sharePermissionRepository.save(permission);
    }

    public List<SharePermission> getCollaborators(Long documentId, User requester) {
        Document doc = findDocumentOrThrow(documentId);
        requireAtLeastViewer(doc, requester);
        return sharePermissionRepository.findByDocumentId(documentId);
    }

    // ---- Access control helpers ----

    private Document findDocumentOrThrow(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    private void requireAtLeastViewer(Document doc, User requester) {
        if (doc.getOwner().getId().equals(requester.getId())) return;

        boolean hasAccess = sharePermissionRepository
                .findByDocumentIdAndUserId(doc.getId(), requester.getId())
                .isPresent();

        if (!hasAccess) {
            throw new SecurityException("You do not have access to this document");
        }
    }

    private void requireAtLeastEditor(Document doc, User requester) {
        if (doc.getOwner().getId().equals(requester.getId())) return;

        SharePermission permission = sharePermissionRepository
                .findByDocumentIdAndUserId(doc.getId(), requester.getId())
                .orElseThrow(() -> new SecurityException("You do not have access to this document"));

        if (permission.getRole() == Role.VIEWER) {
            throw new SecurityException("You only have view access to this document");
        }
    }
}
