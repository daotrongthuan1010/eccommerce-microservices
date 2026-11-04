package com.dtthuan3.ecommerce.catalogservice.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSearchDocument {

    @Field(type = FieldType.Double)
    private Double avg;

    @Field(type = FieldType.Integer)
    private Integer count;
}
