package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.entity.Synonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SynonymRepository
        extends JpaRepository<Synonym, String> {

    List<Synonym> findBySynonymSetIdOrderByTermAscSynonymAsc(
            String synonymSetId
    );

    Optional<Synonym> findBySynonymSetIdAndTermAndSynonym(
            String synonymSetId,
            String term,
            String synonym
    );

    boolean existsBySynonymSetIdAndTermAndSynonym(
            String synonymSetId,
            String term,
            String synonym
    );

    boolean existsBySynonymSetIdAndTermAndSynonymAndIdNot(
            String synonymSetId,
            String term,
            String synonym,
            String id
    );

    void deleteBySynonymSetId(String synonymSetId);
}
