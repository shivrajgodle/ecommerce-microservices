package com.learning.catalog_service.service;

import com.learning.catalog_service.dto.request.ProductRequest;
import com.learning.catalog_service.dto.request.ProductSearchRequest;
import com.learning.catalog_service.dto.response.ProductResponse;
import com.learning.catalog_service.entity.Category;
import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.entity.Tag;
import com.learning.catalog_service.exception.DuplicateResourceException;
import com.learning.catalog_service.exception.ResourceNotFoundException;
import com.learning.catalog_service.mapper.ProductMapper;
import com.learning.catalog_service.repository.CategoryRepository;
import com.learning.catalog_service.repository.ProductRepository;
import com.learning.catalog_service.repository.TagRepository;
import com.learning.catalog_service.repository.spec.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;

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

    /**
     * @Cacheable — BEFORE this method body runs, Spring's proxy checks
     * the "products" cache for a key matching #id. Cache HIT: the method
     * body never executes at all, the cached ProductResponse is returned
     * directly — no repository call, no database round trip. Cache
     * MISS: the method runs normally, and the RETURN VALUE gets stored
     * under that key before being handed back to the caller. Every
     * subsequent call with the same id is a hit until eviction.
     */
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() ->  new ResourceNotFoundException("Product not found with id: "+id));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request){
        if(productRepository.existsBySku(request.getSku())){
            throw new DuplicateResourceException("Product with SKU "+request.getSku() +"already exists");
        }

        Category category = categoryRepository
                .findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found with id: "+request.getCategoryId()));

        Product product = new Product(request.getSku(), request.getName(),request.getDescription(),request.getPrice(),request.getStockQuantity(),category);
        product.setTags(resolveTags(request.getTagNames()));

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    /**
     * @CachePut is DIFFERENT from @Cacheable in one crucial way: it
     * NEVER skips the method body — it always executes, then always
     * overwrites the cache entry with the fresh result. This is exactly
     * what an update needs: you can't check the cache first (that would
     * return STALE data instead of applying the update), but you still
     * want the cache refreshed with the new value instead of just
     * evicted, so the very next getProductById(id) call is still a fast
     * cache hit — with correct data this time.
     */
    @CachePut(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Product not found with id:"+id));

        Category category = categoryRepository
                .findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found with id: "+request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setTags(resolveTags(request.getTagNames()));
        // SKU deliberately not updatable here — treat it as immutable
        // post-creation; changing a SKU has real downstream implications
        // (Cart/Order services may already reference it).

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }


    /**
     * @CacheEvict removes the entry entirely rather than replacing it —
     * appropriate here because after a soft delete, there's no valid
     * "fresh" value to put back into the cache; the next read should
     * miss, hit the database, and correctly discover the product is
     * gone (or filtered out by isActive() in searches).
     */
    @CacheEvict(value = "products", key="#id")
    @Transactional
    public void deleteProduct(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() ->  new ResourceNotFoundException("Product not found with id: "+id));
        product.setActive(false);
        product.setDeleted(true); // soft delete, per BaseEntity's Phase 1 design
        productRepository.save(product);
    }



    private Set<Tag> resolveTags(Set<String> tagNames) {
        if(tagNames == null || tagNames.isEmpty()) return new HashSet<>();

        Set<Tag> existing = new HashSet<>(tagRepository.findByNameIn(tagNames.stream().toList()));
        Set<String> existingNames = existing.stream().map(Tag::getName).collect(Collectors.toSet());

        // Tags not found get CREATED on the fly — a deliberate UX choice
        // for a tagging system (vs. rejecting unknown tags), common for
        // free-form taxonomy fields.
        tagNames.stream().filter(name-> !existingNames.contains(name))
                .forEach(name -> existing.add(tagRepository.save(new Tag(name))));
        return existing;
    }




}
