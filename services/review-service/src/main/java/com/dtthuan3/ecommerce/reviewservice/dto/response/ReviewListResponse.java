package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/** Một dòng trong danh sách đánh giá công khai của sản phẩm (chỉ review APPROVED). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewListResponse {
    private String id;
    private String displayUserName;
    private Integer rating;
    private String comment;
    private boolean verifiedPurchase;
    private boolean anonymous;
    /** true nếu đã từng được chỉnh sửa sau khi đăng. */
    private boolean edited;
    private long helpfulCount;
    /** null nếu người xem chưa đăng nhập. */
    private Boolean likedByMe;
    private List<ReviewResponse.ReviewReplyDto> replies;
    private List<ReviewMediaResponse> media;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
