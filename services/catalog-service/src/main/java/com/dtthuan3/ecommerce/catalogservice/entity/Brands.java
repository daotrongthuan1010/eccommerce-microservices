package com.dtthuan3.ecommerce.catalogservice.entity;


import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Getter
@Setter
@Entity
@Table(
        name = "brands",
        indexes = {
                @Index(name = "idx_brands_slug", columnList = "slug"),
                @Index(name = "idx_brands_status", columnList = "status")
        }
)
public class Brands extends BaseEntity {

    // ID duy nhất của thương hiệu
    @Id
    @GeneratedValue(generator = "brand_uuid_generator")
    @GenericGenerator(
            name = "brand_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "brands_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    // Tên thương hiệu
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Đường dẫn thân thiện dùng cho URL
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    // Mô tả thương hiệu
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Object key của logo được lưu trong MinIO
    @Column(name = "logo_object_key", length = 500)
    private String logoObjectKey;

    // Trạng thái hoạt động của thương hiệu
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BrandsStatus status;
}