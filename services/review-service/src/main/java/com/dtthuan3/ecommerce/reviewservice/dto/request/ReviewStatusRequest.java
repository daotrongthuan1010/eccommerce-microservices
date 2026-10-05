package com.dtthuan3.ecommerce.reviewservice.dto.request;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStatusRequest {
    @NotNull(message = "status không được để trống")
    private ReviewStatus status;
}