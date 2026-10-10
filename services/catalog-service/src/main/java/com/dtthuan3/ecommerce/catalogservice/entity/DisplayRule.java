package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.RuleType;
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
                @Index(name = "idx_display_rules_collection", columnList = "collection_id"),
                @Index(name = "idx_display_rules_status", columnList = "status")
        }
)
@Getter
@Setter
public class DisplayRule extends BaseEntity {

    /** ID duy nhất của quy tắc hiển thị. */
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
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** ID bộ sưu tập mà quy tắc này áp dụng. */
    @Column(name = "collection_id", nullable = false, length = 36)
    private String collectionId;

    /** Loại quy tắc hiển thị. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    /** Cấu hình chi tiết của quy tắc dạng JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> ruleConfig;

    /** Độ ưu tiên khi nhiều quy tắc cùng áp dụng. */
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    /** Trạng thái của quy tắc. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DisplayRuleStatus status;
}
