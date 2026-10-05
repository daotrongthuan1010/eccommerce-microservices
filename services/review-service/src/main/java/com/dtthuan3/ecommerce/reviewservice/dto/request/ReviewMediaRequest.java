package com.dtthuan3.ecommerce.reviewservice.dto.request;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMediaRequest {

    @NotNull(message = "mediaType không được để trống")
    private ReviewMediaType mediaType;

    @NotBlank(message = "mediaUrl không được để trống")
    @Size(
            max = 1000,
            message = "mediaUrl tối đa 1000 ký tự"
    )
    private String mediaUrl;

    @Size(
            max = 1000,
            message = "thumbnailUrl tối đa 1000 ký tự"
    )
    private String thumbnailUrl;

    private Integer sortOrder;
}