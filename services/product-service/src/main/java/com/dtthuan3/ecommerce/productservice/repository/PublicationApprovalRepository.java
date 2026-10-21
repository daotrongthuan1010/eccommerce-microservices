package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.domain.PublicationApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicationApprovalRepository
        extends JpaRepository<PublicationApproval, UUID> {

    List<PublicationApproval> findByRevisionIdOrderBySubmittedAtDesc(
            UUID revisionId
    );

    List<PublicationApproval> findByStatusOrderBySubmittedAtAsc(
            String status
    );

    Optional<PublicationApproval>
    findFirstByRevisionIdAndStatusOrderBySubmittedAtDesc(
            UUID revisionId,
            String status
    );


}
