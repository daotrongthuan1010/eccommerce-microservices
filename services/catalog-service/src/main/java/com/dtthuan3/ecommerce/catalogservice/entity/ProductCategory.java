package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.util.UUID;

@Entity
@Table(
        name = "product_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_category",
                        columnNames = {"product_id", "category_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_categories_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_product_categories_category",
                        columnList = "category_id"
                )
        }
)
@Getter
@Setter
public class ProductCategory extends BaseEntity {

    @Id
    @GeneratedValue(generator = "product_category_uuid_generator")
    @GenericGenerator(
            name = "product_category_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "product_categories_id_seq"
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
     * ID Product thuộc Product Service.
     *
     * Catalog chỉ lưu reference, không sở hữu Product.
     */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * ID Category do Catalog Service sở hữu.
     */
    @Column(
            name = "category_id",
            nullable = false,
            length = 36
    )
    private String categoryId;

    /**
     * Một Product chỉ có tối đa một primary category.
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean primary = false;
}