package com.gwlite.dto;

import lombok.Data;

@Data
public class DocumentRequest {
    private String title;
    private String content;
    private Long folderId; // nullable
}
