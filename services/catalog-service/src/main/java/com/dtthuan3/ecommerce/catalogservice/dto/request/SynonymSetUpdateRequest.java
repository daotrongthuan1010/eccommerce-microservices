package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SynonymSetUpdateRequest {

    @NotBlank(message = "Tên synonym set không được để trống")
    @Size(max = 255, message = "Tên synonym set tối đa 255 ký tự")
    private String name;

    private SynonymSetStatus status;
}