package com.gwlite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The payload published to Redis when a document edit happens on any node.
 * Every node subscribes to "doc:*" channels and re-broadcasts to its own
 * locally-connected WebSocket sessions - this is what makes edits visible
 * across users connected to DIFFERENT app instances.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisEditMessage {
    private Long documentId;
    private String content;
    private String originNodeId; // which app instance published this
}
