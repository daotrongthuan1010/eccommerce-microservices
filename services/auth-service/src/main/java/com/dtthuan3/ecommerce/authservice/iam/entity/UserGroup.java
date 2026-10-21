package com.dtthuan3.ecommerce.authservice.iam.entity;

import com.dtthuan3.ecommerce.authservice.iam.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UserGroup - nhom trong tenant, 2 muc dich: co cau to chuc (cay qua parent_id) va nhom quyen (gan role,
 * thanh vien thua ke). groupType quyet dinh loai nut: PLATFORM, TENANT, DEPARTMENT, TEAM.
 */
@Entity
@Table(
        name = "user_groups",
        indexes = {
            @Index(name = "idx_group_tenant", columnList = "tenant_id"),
            @Index(name = "idx_group_parent", columnList = "parent_id"),
            @Index(name = "idx_group_type", columnList = "group_type_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_type_id")
    private GroupType groupType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UserGroup parent;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
