package com.learning.catalog_service.service;

import com.learning.catalog_service.dto.request.ProductSearchRequest;
import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.repository.ProductRepository;
import com.learning.catalog_service.repository.spec.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<Product> searchProducts(ProductSearchRequest request, Pageable pageable){
        // Specification.where(...) starts a chain; .and(...) composes
        // additional conditions, each independently null-safe per the
        // comments above. Only the filters the caller actually supplied
        // end up in the final SQL WHERE clause.

        Specification<Product> spec = Specification
                .where(ProductSpecifications.isActive())
                .and(ProductSpecifications.hasCategoryId(request.getCategoryId()))
                .and(ProductSpecifications.priceBetween(request.getMinPrice(),request.getMinPrice()))
                .and(ProductSpecifications.nameContains(request.getKeyword()))
                .and(ProductSpecifications.hasAnyTag(request.getTags()));

        // findAll(Specification, Pageable) comes from
        // JpaSpecificationExecutor — this single call handles filtering
        // AND pagination AND sorting (sorting lives inside the Pageable
        // itself) in one query.
        return productRepository.findAll(spec,pageable);

    }

}
