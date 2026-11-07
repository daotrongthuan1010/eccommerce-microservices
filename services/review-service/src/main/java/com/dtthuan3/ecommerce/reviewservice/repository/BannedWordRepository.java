package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.BannedWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {

    /** Kiểm tra trùng (không phân biệt hoa thường). */
    Optional<BannedWord> findByWordIgnoreCase(String word);

    /** Tìm theo từ chứa chuỗi (không phân biệt hoa thường). */
    Page<BannedWord> findByWordContainingIgnoreCase(String word, Pageable pageable);
}
