package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.Manufacturers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManufacturersRepository
        extends JpaRepository<Manufacturers, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );

    List<Manufacturers> findAllByStatusOrderByNameAsc(
            ManufacturerStatus status
    );
}