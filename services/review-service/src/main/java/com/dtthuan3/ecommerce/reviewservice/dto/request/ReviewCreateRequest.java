package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {
    @NotNull(message = "orderItemId không được để trống")
    private Long orderItemId;
    @NotNull(message = "rating không được để trống")
    @Min(value = 1, message = "rating tối thiểu là 1")
    @Max(value = 5, message = "rating tối đa là 5")
    private Integer rating;

    @Size(max = 3000, message = "comment tối đa 3000 ký tự")
    private String comment;
    private Boolean anonymous;
    @Size(max = 5, message = "Tối đa 5 ảnh/video cho một review")
    private List<ReviewMediaRequest> media;
}