package com.dtthuan3.ecommerce.authservice.iam.repository;

import com.dtthuan3.ecommerce.authservice.iam.entity.AuthUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthUserRepository extends JpaRepository<AuthUser, String> {

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByEmail(String email);

    @Query("select u from AuthUser u left join fetch u.tenant left join fetch u.group g"
            + " left join fetch g.groupType left join fetch g.parent left join fetch u.position where u.id = :id")
    Optional<AuthUser> findByIdWithContext(@Param("id") String id);
}
