package com.dtthuan3.ecommerce.catalogservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products", createIndex = false)
@Setting(settingPath = "elasticsearch/catalog-products-settings.json")
public class ProductSearchDocument {

    @Id
    private String productId;

    @Field(type = FieldType.Keyword)
    private String sku;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "catalog_text", searchAnalyzer = "catalog_text"),
            otherFields = @InnerField(suffix = "autocomplete", type = FieldType.Search_As_You_Type, analyzer = "catalog_text")
    )
    private String name;

    private BrandSearchDocument brand;

    private ManufacturerSearchDocument manufacturer;

    private List<CategorySearchDocument> categories;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text, analyzer = "catalog_text", searchAnalyzer = "catalog_text")
    private String searchText;

    private StockSearchDocument stock;

    private RatingSearchDocument rating;

    @Field(type = FieldType.Date)
    private String updatedAt;
}
