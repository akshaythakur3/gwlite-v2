package com.gwlite.repository;

import com.gwlite.model.SharePermission;
import com.gwlite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SharePermissionRepository extends JpaRepository<SharePermission, Long> {
    List<SharePermission> findByDocumentId(Long documentId);
    List<SharePermission> findByUser(User user);
    Optional<SharePermission> findByDocumentIdAndUserId(Long documentId, Long userId);
}
