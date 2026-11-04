package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.HelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HelpfulVoteRepository extends JpaRepository<HelpfulVote, String> {

    Optional<HelpfulVote> findByReviewIdAndUserId(String reviewId, String userId);

    long countByReviewIdAndHelpfulTrue(String reviewId);

    /** Các review mà user đang đặt "hữu ích" = true. */
    @Query("select v.reviewId from HelpfulVote v "
            + "where v.userId = :userId and v.helpful = true and v.reviewId in :reviewIds")
    List<String> findLikedReviewIds(@Param("userId") String userId, @Param("reviewIds") Collection<String> reviewIds);
}

