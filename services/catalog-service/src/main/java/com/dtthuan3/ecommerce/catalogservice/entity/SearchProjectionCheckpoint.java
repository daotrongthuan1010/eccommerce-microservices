package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.CheckpointStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.SourceType;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "search_projection_checkpoints",
        indexes = {
                @Index(name = "idx_search_checkpoint_source", columnList = "source_type")
        }
)
@Getter
@Setter
public class SearchProjectionCheckpoint extends BaseEntity {

    /** ID duy nhất của checkpoint. */
    @Id
    @GeneratedValue(generator = "search_checkpoint_uuid_generator")
    @GenericGenerator(
            name = "search_checkpoint_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "search_projection_checkpoints_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** Loại nguồn dữ liệu mà checkpoint theo dõi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    /** ID event cuối cùng đã xử lý. */
    @Column(name = "last_event_id", length = 36)
    private String lastEventId;

    /** Version của event cuối cùng đã xử lý. */
    @Column(name = "last_event_version")
    private Long lastEventVersion;

    /** Trạng thái xử lý checkpoint. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CheckpointStatus status;
}