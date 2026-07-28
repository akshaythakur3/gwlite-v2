package com.gwlite.controller;

import com.gwlite.dto.DocumentRequest;
import com.gwlite.model.Document;
import com.gwlite.model.User;
import com.gwlite.service.DocumentService;
import com.gwlite.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Document> createDocument(
            @RequestBody DocumentRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.createDocument(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.getDocument(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.updateDocument(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        documentService.deleteDocument(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Document>> getMyDocuments(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.getMyDocuments(user));
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<List<Document>> getSharedWithMe(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.getSharedWithMe(user));
    }
}
