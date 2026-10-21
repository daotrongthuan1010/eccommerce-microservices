package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Manufacturers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManufacturersRepository
        extends JpaRepository<Manufacturers, String> {

    Optional<Manufacturers> findByNameIgnoreCase(
            String name
    );

    boolean existsByNameIgnoreCase(
            String name
    );

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            String id
    );

    Page<Manufacturers> findByStatus(
            ManufacturerStatus status,
            Pageable pageable
    );

    Page<Manufacturers> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    List<Manufacturers> findByStatusOrderByNameAsc(
            ManufacturerStatus status
    );
}