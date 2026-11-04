package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.Valid;
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
public class ReviewUpdateRequest {

    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Integer rating;

    @Size(max = 3000, message = "Nội dung tối đa 3000 ký tự")
    private String content;

    /** Danh sách media MỚI thay thế hoàn toàn bản trước. */
    @Valid
    @Size(max = 6, message = "Tối đa 6 tệp (5 ảnh + 1 video)")
    private List<ReviewMediaRequest> media;

    /** Version FE đang giữ (optimistic locking). */
    @NotNull(message = "version không được để trống")
    private Long version;
}
