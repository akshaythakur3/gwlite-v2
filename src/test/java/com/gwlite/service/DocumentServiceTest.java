package com.gwlite.service;

import com.gwlite.dto.DocumentRequest;
import com.gwlite.model.Document;
import com.gwlite.model.Role;
import com.gwlite.model.SharePermission;
import com.gwlite.model.User;
import com.gwlite.repository.DocumentRepository;
import com.gwlite.repository.FolderRepository;
import com.gwlite.repository.SharePermissionRepository;
import com.gwlite.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private SharePermissionRepository sharePermissionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    private User owner;
    private User stranger;
    private Document document;

    @BeforeEach
    void setUp() {
        owner = new User(1L, "owner@test.com", "Owner", "hashed");
        stranger = new User(2L, "stranger@test.com", "Stranger", "hashed");

        document = new Document();
        document.setId(100L);
        document.setTitle("Test Doc");
        document.setContent("Hello world");
        document.setOwner(owner);
    }

    @Test
    void createDocument_savesWithCorrectOwner() {
        DocumentRequest request = new DocumentRequest();
        request.setTitle("New Doc");
        request.setContent("Some content");

        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.createDocument(request, owner);

        assertEquals("New Doc", result.getTitle());
        assertEquals(owner, result.getOwner());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void getDocument_ownerCanAccess() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        Document result = documentService.getDocument(100L, owner);

        assertEquals(document, result);
    }

    @Test
    void getDocument_strangerWithoutPermissionThrows() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(sharePermissionRepository.findByDocumentIdAndUserId(100L, stranger.getId()))
                .thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> documentService.getDocument(100L, stranger));
    }

    @Test
    void getDocument_sharedViewerCanAccess() {
        SharePermission permission = new SharePermission();
        permission.setRole(Role.VIEWER);

        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(sharePermissionRepository.findByDocumentIdAndUserId(100L, stranger.getId()))
                .thenReturn(Optional.of(permission));

        Document result = documentService.getDocument(100L, stranger);

        assertEquals(document, result);
    }

    @Test
    void updateDocument_viewerCannotEdit() {
        SharePermission permission = new SharePermission();
        permission.setRole(Role.VIEWER);

        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(sharePermissionRepository.findByDocumentIdAndUserId(100L, stranger.getId()))
                .thenReturn(Optional.of(permission));

        DocumentRequest request = new DocumentRequest();
        request.setContent("Trying to edit");

        assertThrows(SecurityException.class,
                () -> documentService.updateDocument(100L, request, stranger));
    }

    @Test
    void updateDocument_editorCanEditAndVersionIncrements() {
        SharePermission permission = new SharePermission();
        permission.setRole(Role.EDITOR);

        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(sharePermissionRepository.findByDocumentIdAndUserId(100L, stranger.getId()))
                .thenReturn(Optional.of(permission));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentRequest request = new DocumentRequest();
        request.setContent("Updated content");

        Document result = documentService.updateDocument(100L, request, stranger);

        assertEquals("Updated content", result.getContent());
        assertEquals(1L, result.getVersion());
    }

    @Test
    void deleteDocument_nonOwnerThrows() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        assertThrows(SecurityException.class,
                () -> documentService.deleteDocument(100L, stranger));

        verify(documentRepository, never()).delete(any());
    }

    @Test
    void deleteDocument_ownerSucceeds() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        documentService.deleteDocument(100L, owner);

        verify(documentRepository, times(1)).delete(document);
    }
}
