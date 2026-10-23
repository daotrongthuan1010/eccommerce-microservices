package com.dtthuan3.ecommerce.reviewservice.dto.request;

import com.dtthuan3.ecommerce.reviewservice.enums.ReviewMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Thứ tự hiển thị = thứ tự phần tử trong mảng (không còn sortOrder mặc định 0 gây trùng). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMediaRequest {

    @NotBlank(message = "File ID không được để trống")
    @Size(max = 36, message = "File ID tối đa 36 ký tự")
    private String fileId;

    @NotNull(message = "Loại media không được để trống")
    private ReviewMediaType mediaType;
}
