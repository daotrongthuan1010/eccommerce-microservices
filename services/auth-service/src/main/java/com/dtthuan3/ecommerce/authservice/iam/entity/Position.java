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

/** Position - chuc danh + trong so tham quyen (level cang cao quyen cang lon). */
@Entity
@Table(
        name = "positions",
        indexes = {
            @Index(name = "idx_position_tenant", columnList = "tenant_id"),
            @Index(name = "idx_position_group", columnList = "group_id"),
            @Index(name = "idx_position_level", columnList = "level")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level;
}
