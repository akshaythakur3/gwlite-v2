package com.gwlite.ot;

import org.springframework.stereotype.Service;

/**
 * Implements a simplified Operational Transform (OT) for plain-text
 * insert/delete operations - the same family of algorithm Google Docs
 * pioneered (Jupiter/OT protocol), reduced to its educational core.
 *
 * THE PROBLEM THIS SOLVES:
 * With plain last-write-wins (what the base project does), if User A and
 * User B both start typing at the same moment, whoever's save reaches the
 * server LAST silently overwrites the other's keystrokes entirely.
 *
 * THE FIX:
 * Instead of sending "here's my whole document now," each client sends
 * small operations ("insert 'x' at position 5"). When two operations were
 * created concurrently (against the same base document version), the server
 * TRANSFORMS one against the other so both can be applied in sequence
 * without losing either person's intent.
 *
 * HONEST SCOPE NOTE: this handles the two most common conflict shapes
 * (concurrent insert-insert and insert-delete) for a SINGLE server applying
 * operations sequentially. Production systems (Google Docs, Figma) use far
 * more complete OT implementations or CRDTs (e.g. Yjs, Automerge) that also
 * handle out-of-order delivery, network partitions, and rich formatting.
 * This is intentionally scoped to demonstrate the core transform logic
 * correctly, not to be a drop-in production OT engine.
 */
@Service
public class OperationalTransformService {

    /**
     * Transforms `opToTransform` against `concurrentOp`, which was already
     * applied to the document. Returns an adjusted operation that, when
     * applied AFTER concurrentOp, produces the correct merged result.
     */
    public TextOperation transform(TextOperation opToTransform, TextOperation concurrentOp) {
        // Case 1: both are inserts
        if (opToTransform.getType() == OpType.INSERT && concurrentOp.getType() == OpType.INSERT) {
            return transformInsertInsert(opToTransform, concurrentOp);
        }
        // Case 2: this is an insert, concurrent one is a delete
        if (opToTransform.getType() == OpType.INSERT && concurrentOp.getType() == OpType.DELETE) {
            return transformInsertAgainstDelete(opToTransform, concurrentOp);
        }
        // Case 3: this is a delete, concurrent one is an insert
        if (opToTransform.getType() == OpType.DELETE && concurrentOp.getType() == OpType.INSERT) {
            return transformDeleteAgainstInsert(opToTransform, concurrentOp);
        }
        // Case 4: both are deletes
        return transformDeleteDelete(opToTransform, concurrentOp);
    }

    private TextOperation transformInsertInsert(TextOperation a, TextOperation b) {
        // If the concurrent insert happened before this one's position, shift right
        if (b.getPosition() < a.getPosition() ||
           (b.getPosition() == a.getPosition() && shouldGoFirst(b, a))) {
            a.setPosition(a.getPosition() + b.getText().length());
        }
        return a;
    }

    private TextOperation transformInsertAgainstDelete(TextOperation insert, TextOperation delete) {
        if (delete.getPosition() < insert.getPosition()) {
            // Deletion happened before our insert point - shift left by whatever was removed
            int shift = Math.min(delete.getLength(), insert.getPosition() - delete.getPosition());
            insert.setPosition(insert.getPosition() - shift);
        }
        return insert;
    }

    private TextOperation transformDeleteAgainstInsert(TextOperation delete, TextOperation insert) {
        if (insert.getPosition() <= delete.getPosition()) {
            // Text was inserted before our delete range - shift right
            delete.setPosition(delete.getPosition() + insert.getText().length());
        }
        // If insert happened INSIDE our delete range, we conservatively keep
        // the delete range as-is rather than trying to split it - documented
        // limitation, a full OT implementation would split the delete op.
        return delete;
    }

    private TextOperation transformDeleteDelete(TextOperation a, TextOperation b) {
        int aStart = a.getPosition();
        int aEnd = a.getPosition() + a.getLength();
        int bStart = b.getPosition();
        int bEnd = b.getPosition() + b.getLength();

        if (bEnd <= aStart) {
            // b's range is entirely before a's - shift a left
            a.setPosition(aStart - b.getLength());
        } else if (bStart >= aEnd) {
            // b's range is entirely after a's - no change needed
        } else {
            // Overlapping delete ranges - shrink `a` by however much overlap
            // already got removed by `b`. Simplified conflict handling.
            int overlapStart = Math.max(aStart, bStart);
            int overlapEnd = Math.min(aEnd, bEnd);
            int overlap = Math.max(0, overlapEnd - overlapStart);
            a.setLength(Math.max(0, a.getLength() - overlap));
            if (bStart < aStart) {
                a.setPosition(Math.max(bStart, aStart - b.getLength()));
            }
        }
        return a;
    }

    /** Tie-breaker when two inserts land at the exact same position: use userId ordering for determinism. */
    private boolean shouldGoFirst(TextOperation earlier, TextOperation later) {
        return earlier.getUserId().compareTo(later.getUserId()) < 0;
    }

    /** Applies a single operation to a string, producing the new content. */
    public String apply(String content, TextOperation op) {
        if (op.getType() == OpType.INSERT) {
            int pos = Math.min(op.getPosition(), content.length());
            return content.substring(0, pos) + op.getText() + content.substring(pos);
        } else {
            int start = Math.min(op.getPosition(), content.length());
            int end = Math.min(op.getPosition() + op.getLength(), content.length());
            if (start >= end) return content;
            return content.substring(0, start) + content.substring(end);
        }
    }
}
