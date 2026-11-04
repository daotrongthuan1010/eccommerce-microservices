package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {

    /** Dòng sản phẩm trong đơn hàng được đánh giá (mỗi dòng chỉ 1 đánh giá). */
    @NotBlank(message = "lineId không được để trống")
    @Size(max = 36, message = "lineId tối đa 36 ký tự")
    private String lineId;

    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Integer rating;

    /** Không bắt buộc: Shopee cho phép chỉ chấm sao. */
    @Size(max = 3000, message = "Nội dung tối đa 3000 ký tự")
    private String content;

    /** Mặc định true (che tên) nếu client không gửi. */
    private Boolean isAnonymous;

    /** Tối đa 5 ảnh + 1 video, kiểm tra chi tiết ở ReviewRules. */
    @Valid
    @Size(max = 6, message = "Tối đa 6 tệp (5 ảnh + 1 video)")
    private List<ReviewMediaRequest> media;
}
