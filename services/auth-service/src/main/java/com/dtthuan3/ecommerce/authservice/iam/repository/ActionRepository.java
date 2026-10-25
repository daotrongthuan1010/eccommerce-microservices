package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.Action;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionRepository extends JpaRepository<Action, Long> {

    Optional<Action> findByCode(String code);

    boolean existsByCode(String code);

    List<Action> findAllByIsDeletedFalseOrderByCodeAsc();
}
