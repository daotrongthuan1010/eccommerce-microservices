package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "synonym_sets",
        indexes = {
                @Index(name = "idx_synonym_sets_status", columnList = "status")
        }
)
@Getter
@Setter
public class SynonymSet extends BaseEntity {

    /** ID duy nhất của tập từ đồng nghĩa. */
    @Id
    @GeneratedValue(generator = "synonym_set_uuid_generator")
    @GenericGenerator(
            name = "synonym_set_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "synonym_sets_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** Tên tập từ đồng nghĩa. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Phiên bản hiện tại của tập từ đồng nghĩa. */
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /** Trạng thái của tập từ đồng nghĩa. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SynonymSetStatus status;
}
