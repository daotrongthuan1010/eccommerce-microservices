package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.AbuseReport;
import com.dtthuan3.ecommerce.reviewservice.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbuseReportRepository extends JpaRepository<AbuseReport, String> {
    Page<AbuseReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    boolean existsByReviewIdAndReporterId(String reviewId, String reporterId);
}
