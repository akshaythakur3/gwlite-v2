package com.gwlite.ot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single atomic edit: insert `text` at `position`, or delete `length`
 * characters starting at `position`. Every keystroke becomes one of these
 * instead of sending the WHOLE document content on every change - this is
 * the foundation Operational Transform is built on.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextOperation {
    private OpType type;
    private int position;
    private String text;   // used for INSERT
    private int length;    // used for DELETE
    private long baseVersion; // the document version this op was created against
    private String userId;
}
