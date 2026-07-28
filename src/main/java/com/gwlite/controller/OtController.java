package com.gwlite.controller;

import com.gwlite.ot.OtDocumentService;
import com.gwlite.ot.TextOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Operation-based editing endpoint (fixes drawback #3).
 *
 * This is an ALTERNATIVE sync path to the full-content WebSocket broadcast
 * in DocumentWebSocketHandler. In a full migration, the frontend editor
 * would send TextOperation objects here (or over a dedicated WS route)
 * instead of the whole textarea content on every keystroke - both smaller
 * payloads AND conflict-safe merging.
 */
@RestController
@RequestMapping("/api/documents/{documentId}/operations")
@RequiredArgsConstructor
public class OtController {

    private final OtDocumentService otDocumentService;

    @PostMapping
    public ResponseEntity<TextOperation> submitOperation(@PathVariable Long documentId,
                                                           @RequestBody TextOperation operation) {
        TextOperation applied = otDocumentService.applyOperation(documentId, operation);
        return ResponseEntity.ok(applied);
    }
}
