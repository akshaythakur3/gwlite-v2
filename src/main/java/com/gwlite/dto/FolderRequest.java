package com.gwlite.dto;

import lombok.Data;

@Data
public class FolderRequest {
    private String name;
    private Long parentFolderId; // nullable
}
