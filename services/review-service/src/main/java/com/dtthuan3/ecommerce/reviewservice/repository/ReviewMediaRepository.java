package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, String> {
    List<ReviewMedia> findByRevisionIdOrderBySortOrderAsc(String revisionId);

    List<ReviewMedia> findByRevisionIdInOrderBySortOrderAsc(Collection<String> revisionIds);
}
