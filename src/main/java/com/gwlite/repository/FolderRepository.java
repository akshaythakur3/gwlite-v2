package com.gwlite.repository;

import com.gwlite.model.Folder;
import com.gwlite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwnerAndParentFolderIsNull(User owner);
    List<Folder> findByParentFolderId(Long parentFolderId);
    List<Folder> findByOwner(User owner);
}
