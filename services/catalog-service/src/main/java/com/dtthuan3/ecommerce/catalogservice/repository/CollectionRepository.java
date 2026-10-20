package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CollectionRepository
        extends JpaRepository<Collection, String> {

    Optional<Collection> findBySlug(
            String slug
    );

    boolean existsBySlug(
            String slug
    );

    boolean existsBySlugAndIdNot(
            String slug,
            String id
    );

    Page<Collection> findByStatus(
            CollectionStatus status,
            Pageable pageable
    );

    Page<Collection> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    List<Collection> findByStatusOrderByDisplayOrderAscIdAsc(
            CollectionStatus status
    );

    @Query("""
            SELECT c
            FROM Collection c
            WHERE c.status = :status
              AND c.startAt <= :now
              AND (
                    c.endAt IS NULL
                    OR c.endAt >= :now
              )
            ORDER BY c.displayOrder ASC, c.id ASC
            """)
    List<Collection> findHomepageCollections(
            CollectionStatus status,
            LocalDateTime now
    );
}
