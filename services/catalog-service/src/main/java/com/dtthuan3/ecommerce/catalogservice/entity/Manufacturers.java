package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "manufacturers",
        indexes = {
                @Index(
                        name = "idx_manufacturers_name",
                        columnList = "name",
                        unique = true
                ),
                @Index(
                        name = "idx_manufacturers_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
public class Manufacturers extends BaseEntity {

    @Id
    @GeneratedValue(generator = "manufacturer_uuid_generator")
    @GenericGenerator(
            name = "manufacturer_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "manufacturers_id_seq"
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
            length = 255,
            unique = true
    )
    private String name;

    @Column(
            name = "country",
            length = 255
    )
    private String country;

    @Column(
            name = "website",
            length = 500
    )
    private String website;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "logo_object_key",
            length = 500
    )
    private String logoObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ManufacturerStatus status;

    @Column(
            name = "revision",
            nullable = false
    )
    private Long revision = 0L;
}