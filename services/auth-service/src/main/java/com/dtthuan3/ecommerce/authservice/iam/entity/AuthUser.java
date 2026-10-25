package com.dtthuan3.ecommerce.authservice.iam.entity;

import com.dtthuan3.ecommerce.authservice.iam.entity.base.BaseEntity;
import com.dtthuan3.ecommerce.authservice.iam.enums.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AuthUser - ban ghi dinh danh noi Keycloak voi IAM. id = Keycloak subject. tenant_id NULL dong nghia System
 * Admin (bo qua moi bo loc tenant).
 */
@Entity
@Table(
        name = "auth_users",
        indexes = {
            @Index(name = "idx_auth_user_tenant", columnList = "tenant_id"),
            @Index(name = "idx_auth_user_username", columnList = "username", unique = true),
            @Index(name = "idx_auth_user_email", columnList = "email", unique = true),
            @Index(name = "idx_auth_user_group", columnList = "group_id"),
            @Index(name = "idx_auth_user_position", columnList = "position_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @Column(name = "is_system_admin", nullable = false)
    @Builder.Default
    private Boolean isSystemAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    public boolean isSystemAdmin() {
        return Boolean.TRUE.equals(isSystemAdmin) && tenant == null;
    }

    public Integer getPositionLevel() {
        return position != null ? position.getLevel() : null;
    }
}
