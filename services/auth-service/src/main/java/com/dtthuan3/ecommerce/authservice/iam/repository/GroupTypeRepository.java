package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.GroupType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupTypeRepository extends JpaRepository<GroupType, Long> {

    Optional<GroupType> findByCode(String code);
}
