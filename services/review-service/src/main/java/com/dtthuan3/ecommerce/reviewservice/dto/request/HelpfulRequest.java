package com.dtthuan3.ecommerce.reviewservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Trạng thái "hữu ích" mong muốn (REV-07): gửi lặp cùng giá trị cho cùng kết quả. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpfulRequest {
    @NotNull(message = "helpful không được để trống")
    private Boolean helpful;
}
