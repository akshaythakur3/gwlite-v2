package com.gwlite.controller;

import com.gwlite.dto.ShareRequest;
import com.gwlite.model.SharePermission;
import com.gwlite.model.User;
import com.gwlite.service.DocumentService;
import com.gwlite.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/share")
@RequiredArgsConstructor
public class ShareController {

    private final DocumentService documentService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<SharePermission> shareDocument(
            @PathVariable Long documentId,
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.shareDocument(documentId, request, user));
    }

    @GetMapping
    public ResponseEntity<List<SharePermission>> getCollaborators(
            @PathVariable Long documentId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(documentService.getCollaborators(documentId, user));
    }
}
