package com.learning.catalog_service.controller;

import com.learning.catalog_service.dto.request.BulkStockDecrementRequest;
import com.learning.catalog_service.dto.request.ProductRequest;
import com.learning.catalog_service.dto.request.ProductSearchRequest;
import com.learning.catalog_service.dto.response.ApiResponse;
import com.learning.catalog_service.dto.response.ProductResponse;
import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping
    public ResponseEntity<Page<Product>> search(
            @ModelAttribute ProductSearchRequest request,
            @PageableDefault(size = 20,sort = "names")Pageable pageable){
        return ResponseEntity.ok(productService.searchProducts(request,pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id){
        ProductResponse result = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Product retrieved",result));
    }

    @PostMapping
   public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request){
        ProductResponse result = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED.value(), "Product Created",result));
   }

   @PutMapping("/{id}")
   public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request){
        ProductResponse result = productService.updateProduct(id,request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Product Updated",result));
   }

   @DeleteMapping("/{id}")
   public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id){
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),"Product deleted",null));
   }

   @PatchMapping("/decrement-stock-bulk")
   public ResponseEntity<ApiResponse<Void>> decrementStockBulk(@Valid @RequestBody BulkStockDecrementRequest request){
        productService.decrementStockBulk(request.getItems());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),"Stock Decremented",null));
   }



}
