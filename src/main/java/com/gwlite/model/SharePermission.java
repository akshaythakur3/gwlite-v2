package com.gwlite.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * This IS the RBAC "Document_User" table (drawback #5): a many-to-many join
 * between documents and users with an attached role, enforced server-side
 * in DocumentService (requireAtLeastViewer / requireAtLeastEditor) rather
 * than trusted from the client. Named share_permissions here, but this is
 * the same pattern - access isn't a boolean, it's role-scoped per user per
 * document.
 */
@Entity
@Table(name = "share_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"document_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

}
