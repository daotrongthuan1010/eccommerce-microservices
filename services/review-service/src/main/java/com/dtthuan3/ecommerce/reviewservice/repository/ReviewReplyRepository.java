package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ReviewReply;
import com.dtthuan3.ecommerce.reviewservice.enums.ReplyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, String> {
    List<ReviewReply> findByReviewIdInAndStatusOrderByCreatedAtAsc(Collection<String> reviewIds, ReplyStatus status);

    Optional<ReviewReply> findFirstByReviewIdAndStatusOrderByCreatedAtDesc(String reviewId, ReplyStatus status);
}
