package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.entity.DisplayRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisplayRuleRepository
        extends JpaRepository<DisplayRule, String> {

    List<DisplayRule>
    findByCollectionIdOrderByPriorityAscIdAsc(
            String collectionId
    );

    List<DisplayRule>
    findByCollectionIdAndStatusOrderByPriorityAscIdAsc(
            String collectionId,
            DisplayRuleStatus status
    );

    boolean existsByCollectionIdAndPriority(
            String collectionId,
            Integer priority
    );

    boolean existsByCollectionIdAndPriorityAndIdNot(
            String collectionId,
            Integer priority,
            String id
    );

    boolean existsByCollectionId(
            String collectionId
    );
}