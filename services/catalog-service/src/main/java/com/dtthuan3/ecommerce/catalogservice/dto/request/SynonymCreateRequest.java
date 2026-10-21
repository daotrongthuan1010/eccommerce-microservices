package com.dtthuan3.ecommerce.catalogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SynonymCreateRequest {

    @NotBlank(message = "Term không được để trống")
    @Size(max = 255, message = "Term tối đa 255 ký tự")
    private String term;

    @NotBlank(message = "Synonym không được để trống")
    @Size(max = 255, message = "Synonym tối đa 255 ký tự")
    private String synonym;
}
