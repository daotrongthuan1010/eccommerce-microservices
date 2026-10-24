package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductExistsResponse {

    private UUID id;
}