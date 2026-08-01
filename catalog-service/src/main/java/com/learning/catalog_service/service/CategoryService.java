package com.learning.catalog_service.service;

import com.learning.catalog_service.entity.Category;
import com.learning.catalog_service.exception.DuplicateResourceException;
import com.learning.catalog_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Categories change RARELY (an admin adding one occasionally) and
    // are read CONSTANTLY (every product page, every filter dropdown) —
    // close to the ideal caching candidate. No key needed since there
    // are no parameters; the whole list is cached as a single entry.
    @Cacheable("activeCategories")
    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    // Evicting the WHOLE "activeCategories" cache (no key specified,
    // allEntries = true) rather than a single key — appropriate because
    // adding a category invalidates the entire cached LIST, not one
    // entry within it.
    @CacheEvict(value = "activeCategories", allEntries = true)
    public Category createCategory(String name , String description) {
        if(categoryRepository.existsByName(name)){
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        return categoryRepository.save(new Category(name, description));
    }
}
