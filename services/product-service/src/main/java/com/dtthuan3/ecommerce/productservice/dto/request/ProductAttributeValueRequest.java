package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ProductAttributeValueRequest {

        @NotNull(message = "ID thuộc tính không được để trống")
        private UUID attributeId;

        private UUID optionId;

        private String valueText;

        private BigDecimal valueNumber;

        private LocalDate valueDate;

        private Boolean valueBoolean;
}