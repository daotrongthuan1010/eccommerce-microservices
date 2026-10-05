package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpfulResponse {

    private Long reviewId;

    private Long helpfulCount;

    private boolean helpfulByCurrentUser;
}