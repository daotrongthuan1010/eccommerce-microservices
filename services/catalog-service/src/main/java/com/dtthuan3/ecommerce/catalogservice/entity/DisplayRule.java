package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleType;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(
        name = "display_rules",
        indexes = {
                @Index(
                        name = "idx_display_rules_collection_id",
                        columnList = "collection_id"
                ),
                @Index(
                        name = "idx_display_rules_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_display_rules_collection_priority",
                        columnList = "collection_id, priority"
                )
        }
)
@Getter
@Setter
public class DisplayRule extends BaseEntity {

    @Id
    @GeneratedValue(generator = "display_rule_uuid_generator")
    @GenericGenerator(
            name = "display_rule_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "display_rules_id_seq"
                    )
            }
    )
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String id;

    /**
     * Không dùng @ManyToOne.
     * Chỉ lưu ID của Collection.
     */
    @Column(
            name = "collection_id",
            nullable = false,
            length = 36
    )
    private String collectionId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "rule_type",
            nullable = false,
            length = 50
    )
    private DisplayRuleType ruleType;

    /**
     * PostgreSQL JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "rule_config",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private Map<String, Object> ruleConfig;

    /**
     * Rule có priority nhỏ hơn sẽ được xử lý trước.
     */
    @Column(
            name = "priority",
            nullable = false
    )
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private DisplayRuleStatus status;

    @Column(
            name = "revision",
            nullable = false
    )
    private Long revision = 0L;
}