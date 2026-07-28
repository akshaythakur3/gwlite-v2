package com.gwlite.controller;

import com.gwlite.dto.FolderRequest;
import com.gwlite.model.Folder;
import com.gwlite.model.User;
import com.gwlite.service.FolderService;
import com.gwlite.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Folder> createFolder(@RequestBody FolderRequest request,
                                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(folderService.createFolder(request, user));
    }

    @GetMapping("/root")
    public ResponseEntity<List<Folder>> getRootFolders(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(folderService.getRootFolders(user));
    }

    @GetMapping("/{parentId}/subfolders")
    public ResponseEntity<List<Folder>> getSubFolders(@PathVariable Long parentId) {
        return ResponseEntity.ok(folderService.getSubFolders(parentId));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        folderService.deleteFolder(folderId, user);
        return ResponseEntity.noContent().build();
    }
}
