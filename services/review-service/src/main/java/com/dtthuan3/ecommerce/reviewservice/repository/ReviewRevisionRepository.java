package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ReviewRevision;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRevisionRepository extends JpaRepository<ReviewRevision, String> {
    /** Lấy reviewId mà KHÔNG nạp revision vào persistence context (để khóa review trước rồi mới đọc revision). */
    @Query("select r.reviewId from ReviewRevision r where r.id = :id")
    Optional<String> findReviewIdById(@Param("id") String id);

    long countByStatus(RevisionStatus status);

    Optional<ReviewRevision> findFirstByStatusOrderBySubmittedAtAsc(RevisionStatus status);

    Optional<ReviewRevision> findByReviewIdAndRevisionNo(String reviewId, Integer revisionNo);

    /** Hàng chờ kiểm duyệt (cũ nhất trước). */
    Page<ReviewRevision> findByStatusOrderBySubmittedAtAsc(RevisionStatus status, Pageable pageable);
}
