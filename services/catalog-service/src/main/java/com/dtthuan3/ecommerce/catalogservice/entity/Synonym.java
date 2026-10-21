package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "synonyms",
        indexes = {
                @Index(name = "idx_synonyms_set", columnList = "synonym_set_id"),
                @Index(name = "idx_synonyms_term", columnList = "term")
        }
)
@Getter
@Setter

// tu đồng nghĩa
public class Synonym extends BaseEntity {

    /** ID duy nhất của bản ghi từ đồng nghĩa. */
    @Id
    @GeneratedValue(generator = "synonym_uuid_generator")
    @GenericGenerator(
            name = "synonym_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "synonyms_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** ID của tập từ đồng nghĩa. */
    @Column(name = "synonym_set_id", nullable = false, length = 36)
    private String synonymSetId;

    /** Từ khóa gốc cần tìm kiếm. */
    @Column(name = "term", nullable = false, length = 255)
    private String term;

    /** Từ hoặc cụm từ được xem là tương đương. */
    @Column(name = "synonym", nullable = false, length = 255)
    private String synonym;
}
