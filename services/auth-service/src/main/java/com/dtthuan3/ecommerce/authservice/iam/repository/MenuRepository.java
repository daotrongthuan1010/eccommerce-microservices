package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.Menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByCode(String code);

    boolean existsByCode(String code);

    List<Menu> findAllByIsDeletedFalseOrderByOrderNoAscIdAsc();

    @Query("select m from Menu m left join fetch m.parent where m.isDeleted = false and m.isActive = true order by m.orderNo asc, m.id asc")
    List<Menu> findAllActiveWithParent();
}
