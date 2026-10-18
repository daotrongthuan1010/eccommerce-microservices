package com.dtthuan3.ecommerce.catalogservice.entity;


import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "brands",
        indexes = {
                @Index(name = "idx_brands_slug", columnList = "slug", unique = true),
                @Index(name = "idx_brands_status", columnList = "status")
        }
)
@Getter
@Setter
public class Brands extends BaseEntity {

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

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_object_key", length = 500)
    private String logoObjectKey;

    @Column(name = "website", length = 500)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BrandsStatus status;

    @Column(name = "revision", nullable = false)
    private Long revision = 0L;

}