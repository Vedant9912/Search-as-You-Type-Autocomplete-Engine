package com.autocomplete.autocompleteengine.repository;

import com.autocomplete.autocompleteengine.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Naive baseline search (Phase 1) - used to benchmark against the Trie.
    @Query("SELECT p FROM Product p WHERE p.name LIKE CONCAT(:prefix, '%') ORDER BY p.popularity DESC")
    List<Product> findByNaivePrefixSearch(@Param("prefix") String prefix, Pageable pageable);
}
