package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateProductDraftRequest {

        @NotBlank(message = "Mã sản phẩm không được để trống")
        @Size(max = 100, message = "Mã sản phẩm không được vượt quá 100 ký tự")
        private String productCode;

        @NotNull(message = "Danh mục sản phẩm không được để trống")
        private UUID categoryId;

        private UUID brandId;

        private UUID manufacturerId;

        private UUID attributeTemplateId;

        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
        private String name;

        private String shortDescription;

        private String description;

        private String originCountry;

        private String storageCondition;

        private String deliveryCondition;

        private String returnPolicy;

        @Valid
        private List<ProductAttributeValueRequest> attributes =
                new ArrayList<>();
}