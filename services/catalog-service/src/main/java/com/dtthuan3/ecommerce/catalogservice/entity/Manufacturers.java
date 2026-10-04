package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "manufacturers")
public class Manufacturers {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "manufactures_seq",
            sequenceName = "manufacturers_id_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(name = "country", nullable = false, length = 50)
    private String country;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website", nullable = false)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ManufacturerStatus status;
}
