package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReplyRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 2000, message = "Phản hồi tối đa 2000 ký tự")
    private String reply;
}