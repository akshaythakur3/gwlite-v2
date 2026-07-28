package com.gwlite.dto;

import com.gwlite.model.Role;
import lombok.Data;

@Data
public class ShareRequest {
    private String userEmail;
    private Role role; // EDITOR or VIEWER
}
