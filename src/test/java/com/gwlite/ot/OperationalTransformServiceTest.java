package com.gwlite.ot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalTransformServiceTest {

    private final OperationalTransformService service = new OperationalTransformService();

    @Test
    void concurrentInserts_atDifferentPositions_bothPreserved() {
        // Base content: "Hello World"
        // User A inserts "X" at position 0 -> "XHello World"
        // User B (concurrently, unaware of A's edit) inserts "Y" at position 6 -> "Hello YWorld"
        // After transform, B's op should shift right by 1 to account for A's insert
        TextOperation opA = new TextOperation(OpType.INSERT, 0, "X", 0, 1, "userA");
        TextOperation opB = new TextOperation(OpType.INSERT, 6, "Y", 0, 1, "userB");

        TextOperation transformedB = service.transform(opB, opA);

        assertEquals(7, transformedB.getPosition()); // shifted right by len("X")
    }

    @Test
    void applyInsert_insertsAtCorrectPosition() {
        String result = service.apply("Hello World", new TextOperation(OpType.INSERT, 5, ",", 0, 1, "u1"));
        assertEquals("Hello, World", result);
    }

    @Test
    void applyDelete_removesCorrectRange() {
        String result = service.apply("Hello World", new TextOperation(OpType.DELETE, 5, null, 6, 1, "u1"));
        assertEquals("Hello", result);
    }

    @Test
    void insertAgainstEarlierDelete_shiftsLeft() {
        // Concurrent delete removed 3 chars starting at position 0.
        // An insert originally targeting position 10 must shift left by 3.
        TextOperation insert = new TextOperation(OpType.INSERT, 10, "X", 0, 1, "userA");
        TextOperation delete = new TextOperation(OpType.DELETE, 0, null, 3, 1, "userB");

        TextOperation transformed = service.transform(insert, delete);

        assertEquals(7, transformed.getPosition());
    }

    @Test
    void deleteAgainstEarlierInsert_shiftsRight() {
        // Concurrent insert added 4 chars at position 0.
        // A delete originally targeting position 5 must shift right by 4.
        TextOperation delete = new TextOperation(OpType.DELETE, 5, null, 2, 1, "userA");
        TextOperation insert = new TextOperation(OpType.INSERT, 0, "test", 0, 1, "userB");

        TextOperation transformed = service.transform(delete, insert);

        assertEquals(9, transformed.getPosition());
    }

    @Test
    void overlappingDeletes_shrinkToAvoidDoubleDelete() {
        // Both users try to delete overlapping ranges [2,8) and [5,10)
        TextOperation opA = new TextOperation(OpType.DELETE, 2, null, 6, 1, "userA"); // [2,8)
        TextOperation opB = new TextOperation(OpType.DELETE, 5, null, 5, 1, "userB"); // [5,10) - already applied

        TextOperation transformed = service.transform(opA, opB);

        // Overlap [5,8) = 3 chars already removed by opB; opA's effective length shrinks
        assertEquals(3, transformed.getLength());
    }
}
