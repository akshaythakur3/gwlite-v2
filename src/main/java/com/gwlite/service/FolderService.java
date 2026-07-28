package com.gwlite.service;

import com.gwlite.dto.FolderRequest;
import com.gwlite.model.Folder;
import com.gwlite.model.User;
import com.gwlite.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    public Folder createFolder(FolderRequest request, User owner) {
        Folder folder = new Folder();
        folder.setName(request.getName());
        folder.setOwner(owner);

        if (request.getParentFolderId() != null) {
            Folder parent = folderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
            folder.setParentFolder(parent);
        }

        return folderRepository.save(folder);
    }

    public List<Folder> getRootFolders(User owner) {
        return folderRepository.findByOwnerAndParentFolderIsNull(owner);
    }

    public List<Folder> getSubFolders(Long parentFolderId) {
        return folderRepository.findByParentFolderId(parentFolderId);
    }

    public void deleteFolder(Long folderId, User requester) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (!folder.getOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Only the owner can delete this folder");
        }

        folderRepository.delete(folder);
    }
}
