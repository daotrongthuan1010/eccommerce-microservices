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
                @Index(
                        name = "idx_synonym_sets_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_synonym_sets_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter

// bộ từ đồng nghĩa
public class SynonymSet extends BaseEntity {

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
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String id;

    @Column(
            name = "name",
            nullable = false,
            length = 255
    )
    private String name;

    /**
     * Business version của Synonym Set.
     *
     * Ví dụ:
     * electronics-v1 -> version 1
     * sau khi thay đổi synonym -> version 2
     */
    @Column(
            name = "version",
            nullable = false
    )
    private Long version = 1L;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private SynonymSetStatus status;
}