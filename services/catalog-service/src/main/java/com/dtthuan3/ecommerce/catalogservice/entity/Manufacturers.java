package com.dtthuan3.ecommerce.catalogservice.entity;


import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
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
        name = "manufacturers",
        indexes = {
                @Index(name = "idx_manufacturers_status", columnList = "status"),
                @Index(name = "idx_manufacturers_country", columnList = "country")
        }
)
public class Manufacturers extends BaseEntity {

    // ID duy nhất của nhà sản xuất
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
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    // Tên nhà sản xuất
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Quốc gia của nhà sản xuất
    @Column(name = "country", length = 100)
    private String country;

    // Mô tả nhà sản xuất
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Website chính thức của nhà sản xuất
    @Column(name = "website", length = 500)
    private String website;

    // Trạng thái hoạt động của nhà sản xuất
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ManufacturerStatus status;
}