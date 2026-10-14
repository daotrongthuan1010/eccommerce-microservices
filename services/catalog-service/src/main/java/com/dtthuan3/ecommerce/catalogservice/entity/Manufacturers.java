package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "manufacturers",
        indexes = {
                @Index(
                        name = "idx_manufacturers_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_manufacturers_name",
                        columnList = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manufacturers extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "manufacturers_seq"
    )
    @SequenceGenerator(
            name = "manufacturers_seq",
            sequenceName = "manufacturers_id_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(
            name = "name",
            nullable = false,
            length = 255
    )
    private String name;

    @Column(
            name = "country",
            nullable = false,
            length = 50
    )
    private String country;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "website",
            length = 500
    )
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private ManufacturerStatus status =
            ManufacturerStatus.ACTIVE;
}