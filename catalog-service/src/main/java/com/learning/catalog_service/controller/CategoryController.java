package com.learning.catalog_service.controller;

import com.learning.catalog_service.dto.request.CategoryRequest;
import com.learning.catalog_service.dto.response.ApiResponse;
import com.learning.catalog_service.dto.response.CategoryResponse;
import com.learning.catalog_service.entity.Category;
import com.learning.catalog_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getActiveCategories() {
        List<Category> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Categories retrieved", categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@Valid @RequestBody CategoryRequest category) {
        Category createdCategory = categoryService.createCategory(category.getName(),category.getDescription());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED.value(), "Category created", createdCategory));
    }

}
