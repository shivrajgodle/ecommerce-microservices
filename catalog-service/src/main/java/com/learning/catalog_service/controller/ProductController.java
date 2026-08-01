package com.learning.catalog_service.controller;

import com.learning.catalog_service.dto.request.ProductSearchRequest;
import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * @ModelAttribute binds query params (?categoryId=3&minPrice=10...)
     * onto ProductSearchRequest's fields automatically — no manual
     * parsing. @PageableDefault supplies fallback pagination/sort
     * behavior when the client doesn't specify ?page=/?size=/?sort=.
     *
     * A real request might look like:
     * GET /api/v1/products?categoryId=3&minPrice=20&maxPrice=100&sort=price,asc&page=0&size=20
     */

    public ResponseEntity<Page<Product>> search(
            @ModelAttribute ProductSearchRequest request,
            @PageableDefault(size = 20,sort = "names")Pageable pageable){
        return ResponseEntity.ok(productService.searchProducts(request,pageable));
    }

}
