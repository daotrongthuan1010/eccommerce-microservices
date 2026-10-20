package com.dtthuan3.ecommerce.catalogservice.dto.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollectionReorderRequest {

    @NotEmpty(
            message = "Danh sách collection không được để trống"
    )
    private List<String> collectionIds;
}