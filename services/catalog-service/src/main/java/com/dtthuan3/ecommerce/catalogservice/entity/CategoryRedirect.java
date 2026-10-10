package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryRedirectStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "category_redirects",
        indexes = {
                @Index(name = "idx_category_redirects_old_path", columnList = "old_path"),
                @Index(name = "idx_category_redirects_status", columnList = "status")
        }
)
@Getter
@Setter
public class CategoryRedirect extends BaseEntity {

    /** ID duy nhất của bản ghi redirect. */
    @Id
    @GeneratedValue(generator = "category_redirect_uuid_generator")
    @GenericGenerator(
            name = "category_redirect_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "category_redirects_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** URL hoặc path cũ của danh mục. */
    @Column(name = "old_path", nullable = false, length = 1000)
    private String oldPath;

    /** URL hoặc path mới mà người dùng được chuyển hướng tới. */
    @Column(name = "new_path", nullable = false, length = 1000)
    private String newPath;

    /** Trạng thái của redirect. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CategoryRedirectStatus status;
}
