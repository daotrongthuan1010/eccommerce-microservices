package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ModerationDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModerationDecisionRepository extends JpaRepository<ModerationDecision, String> {
    /** Quyết định gần nhất của một revision (để chủ review xem lý do bị từ chối). */
    Optional<ModerationDecision> findFirstByRevisionIdOrderByDecidedAtDesc(String revisionId);
}