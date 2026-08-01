package com.learning.catalog_service.repository;

import com.learning.catalog_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {
    Optional<Category> findByName(String name);
    List<Category> findByActiveTrue();
    boolean existsByName(String name);
}
