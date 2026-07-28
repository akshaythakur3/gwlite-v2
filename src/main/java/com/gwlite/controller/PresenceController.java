package com.gwlite.controller;

import com.gwlite.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/documents/{documentId}/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping
    public ResponseEntity<Set<String>> getActiveViewers(@PathVariable Long documentId) {
        return ResponseEntity.ok(presenceService.getActiveViewers(documentId));
    }
}
