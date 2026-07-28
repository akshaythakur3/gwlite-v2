package com.gwlite.repository;

import com.gwlite.model.Document;
import com.gwlite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwner(User owner);
    List<Document> findByFolderId(Long folderId);
    List<Document> findByOwnerAndFolderIsNull(User owner);
}
