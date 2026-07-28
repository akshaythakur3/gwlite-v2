package com.gwlite.ot;

import com.gwlite.model.Document;
import com.gwlite.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side authority for operation-based collaborative editing.
 * Keeps a short rolling history of applied operations per document so that
 * an incoming operation (created against an older `baseVersion`) can be
 * transformed against everything that happened since, then applied cleanly.
 *
 * This is the "OT server" role - in the Jupiter/Google Wave OT protocol,
 * the server is the single source of truth that resolves ordering, and
 * clients only ever transform against the server's canonical history.
 */
@Service
@RequiredArgsConstructor
public class OtDocumentService {

    private final DocumentRepository documentRepository;
    private final OperationalTransformService transformService;

    // documentId -> ordered history of operations applied since the doc was loaded into memory
    private final Map<Long, List<TextOperation>> operationHistory = new ConcurrentHashMap<>();

    /**
     * Applies an incoming operation to the document, transforming it against
     * any operations that happened after its baseVersion first.
     * Returns the transformed operation actually applied (send this to peers,
     * NOT the client's original op, since positions may have shifted).
     */
    public synchronized TextOperation applyOperation(Long documentId, TextOperation incoming) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        List<TextOperation> history = operationHistory.computeIfAbsent(documentId, id -> new CopyOnWriteArrayList<>());

        TextOperation transformed = incoming;
        for (TextOperation concurrentOp : history) {
            if (concurrentOp.getBaseVersion() >= incoming.getBaseVersion()) {
                transformed = transformService.transform(transformed, concurrentOp);
            }
        }

        String newContent = transformService.apply(doc.getContent(), transformed);
        doc.setContent(newContent);
        doc.setVersion(doc.getVersion() + 1);
        documentRepository.save(doc);

        transformed.setBaseVersion(doc.getVersion());
        history.add(transformed);

        // Keep history bounded - only need recent ops for transform purposes
        if (history.size() > 200) {
            history.remove(0);
        }

        return transformed;
    }

    public void clearHistory(Long documentId) {
        operationHistory.remove(documentId);
    }
}
