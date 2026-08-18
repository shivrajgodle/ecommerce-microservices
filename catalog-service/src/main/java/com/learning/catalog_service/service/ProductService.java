package com.learning.catalog_service.service;

import com.learning.catalog_service.dto.request.ProductRequest;
import com.learning.catalog_service.dto.request.ProductSearchRequest;
import com.learning.catalog_service.dto.request.StockDecrementItem;
import com.learning.catalog_service.dto.response.BulkUploadResult;
import com.learning.catalog_service.dto.response.ProductResponse;
import com.learning.catalog_service.dto.response.RowError;
import com.learning.catalog_service.entity.Category;
import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.entity.Tag;
import com.learning.catalog_service.exception.DuplicateResourceException;
import com.learning.catalog_service.exception.InsufficientStockException;
import com.learning.catalog_service.exception.ResourceNotFoundException;
import com.learning.catalog_service.mapper.ProductMapper;
import com.learning.catalog_service.repository.CategoryRepository;
import com.learning.catalog_service.repository.ProductRepository;
import com.learning.catalog_service.repository.TagRepository;
import com.learning.catalog_service.repository.spec.ProductSpecifications;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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


    /**
     * DELIBERATELY split into two methods — this is not a stylistic choice,
     * it's REQUIRED for correct behavior, and it's a genuinely important
     * Spring internals gotcha worth understanding precisely.
     * Both @Retryable and @Transactional work via AOP PROXIES wrapping the
     * bean. If a single method carried BOTH annotations, only ONE proxy
     * layer would actually take effect in the way you'd expect for retry
     * semantics: @Transactional's proxy would open ONE transaction before
     * @Retryable's logic runs, meaning every retry attempt would execute
     * inside the SAME already-partially-failed transaction — which, after
     * the first OptimisticLockException, is likely already marked
     * rollback-only by Spring, making subsequent "retries" fail immediately
     * and uselessly.
     *
     * Splitting it means: THIS outer method is retried by Spring Retry, and
     * EACH retry attempt calls the inner method fresh — which, being a
     * SEPARATE proxied bean method call, opens a BRAND NEW transaction each
     * time. This is exactly what you want: attempt 1 fails and rolls back
     * cleanly, attempt 2 starts completely fresh with newly-read @Version
     * values, attempt 3 likewise.
     */
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2
            )
    )
    public void decrementStockBulk(List<StockDecrementItem> items){
        decrementStockBulkInternals(items);
    }

    @Transactional
    protected void decrementStockBulkInternals(List<StockDecrementItem> items) {
        for(StockDecrementItem item : items){
            Product product = productRepository.findById(item.getProductId()).orElseThrow(()-> new ResourceNotFoundException("Product not found: "+item.getProductId()));

            if(product.getStockQuantity() < item.getQuantity()){
                // A genuine business rule failure (not enough stock) is NOT
                // retried — retrying a legitimately-insufficient-stock
                // situation would never succeed no matter how many times
                // you try. Only CONCURRENT MODIFICATION conflicts
                // (ObjectOptimisticLockingFailureException) are worth
                // retrying — that's specifically why retryFor targets only
                // that one exception type above, not a broad catch-all.
                throw new InsufficientStockException("Insufficient stock for product '"+product.getName()+"'- requested " + item.getQuantity()+",available " + product.getStockQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
            // The actual OptimisticLockException, if it happens, is thrown
            // here (or at flush/commit) — Hibernate compares the @Version
            // it read at the top of this loop against what's currently in
            // the database and finds they no longer match, because some
            // OTHER transaction updated this exact product in between.
        }
        // If EVERY item in this list succeeds, the @Transactional method
        // returns normally and the whole batch commits atomically — all
        // rows updated together, in ONE database transaction, because every
        // Product row lives in the SAME database (catalog_db) that this
        // service owns outright. This is worth contrasting sharply with the
        // Order->Payment saga: THAT needed Kafka choreography specifically
        // because it spans two SEPARATE databases with no shared
        // transaction possible. Multi-row atomicity within ONE service's
        // own database is just a normal ACID transaction — no saga
        // machinery needed at all. Recognizing when you DON'T need a saga
        // is just as important as knowing how to build one.
    }

    /**
     * Expected columns, in order, WITH A HEADER ROW (row 0) that's skipped:
     * A: SKU | B: Name | C: Description | D: Price | E: StockQuantity
     * F: CategoryName | G: Tags (comma-separated)
     *
     * UPSERT BY SKU: if a row's SKU already exists, that product is
     * UPDATED, not rejected as a duplicate. This is the correct default
     * for bulk imports specifically — it makes the operation IDEMPOTENT.
     * If someone's upload fails partway through (network blip, they
     * close their laptop), they can just re-run the exact same file
     * without manually figuring out which rows already made it in. This
     * is a genuinely different semantic from the single-product
     * createProduct() endpoint (Phase E), which correctly REJECTS a
     * duplicate SKU — a single explicit "create" action duplicating
     * should be an error; a bulk "load this data" operation being
     * safely re-runnable is a feature.
     */
    @Transactional
    public BulkUploadResult bulkUpload(MultipartFile file){
        List<RowError> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows;

        try (InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is)){

                Sheet sheet = workbook.getSheetAt(0);
                // getLastRowNum() is 0-indexed and EXCLUDES the header, so this
                // is exactly the count of data rows, not an off-by-one guess.
                totalRows = sheet.getLastRowNum();

                for(int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++){
                    Row row = sheet.getRow(rowNum);
                    if(row == null || isRowEmpty(row)) continue;

                    String sku = null;
                    try{
                        sku = getCellString(row,0);
                        String name = getCellString(row, 1);
                        String description = getCellString(row, 2);
                        BigDecimal price = BigDecimal.valueOf(getCellNumeric(row, 3));
                        Integer stock = (int) getCellNumeric(row, 4);
                        String categoryName = getCellString(row, 5);
                        String tagsCell = getCellString(row, 6);

                        // ROW-LEVEL VALIDATION, done manually here rather than
                        // via @Valid on a DTO — there's no HTTP request body
                        // for Bean Validation to bind against; each row is
                        // effectively its own tiny "request" we validate by hand.
                        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU is required");
                        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
                        if (price.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Price must be positive");

                        Category category = categoryRepository.findByName(categoryName)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + categoryName));

                        Set<String> tagNames = tagsCell == null || tagsCell.isBlank()
                        ? Set.of() : Arrays.stream(tagsCell.split(",")).map(String::trim).collect(java.util.stream.Collectors.toSet());

                        Product product = productRepository.findBySku(sku).orElse(null);
                        if (product == null) {
                            product = new Product(sku, name, description, price, stock, category);
                        } else {
                            product.setName(name);
                            product.setDescription(description);
                            product.setPrice(price);
                            product.setStockQuantity(stock);
                            product.setCategory(category);
                        }
                        product.setTags(resolveTags(tagNames));
                        productRepository.save(product);
                        successCount++;
                    } catch(Exception rowEx){
                        // CAUGHT PER ROW, not allowed to propagate. This is what
                        // makes partial success possible — one bad row's
                        // exception is recorded and processing CONTINUES to the
                        // next row, rather than aborting the whole loop (and,
                        // combined with the class-level @Transactional, rolling
                        // back every row already processed in this same
                        // request). Worth being explicit that this means a
                        // single Spring @Transactional method is doing manual,
                        // fine-grained error handling INSIDE itself rather than
                        // relying on the transaction boundary to enforce
                        // all-or-nothing — a deliberate, atypical use of
                        // @Transactional here (it's still useful for ensuring
                        // the WHOLE batch commits together on full success, or
                        // rolls back together if something outside this loop —
                        // like the file itself being unreadable — fails).
                        errors.add(RowError.builder()
                        .rowNumber(rowNum + 1) // +1 so error messages match what a person sees in Excel (1-indexed, header is row 1)
                        .sku(sku)
                        .reason(rowEx.getMessage())
                        .build());
                    }
                }            

            } catch(Exception e){
            throw new IllegalArgumentException("Could not read the uploaded file — is it a valid .xlsx?", e);
        }
         return BulkUploadResult.builder()
            .totalRows(totalRows)
            .successCount(successCount)
            .failureCount(errors.size())
            .errors(errors)
            .build();
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if(cell == null) return null;
        // Excel doesn't strictly enforce a column's "type" — a SKU column
        // might contain "1001" typed as a NUMBER cell rather than TEXT if
        // someone wasn't careful formatting the spreadsheet. Handling both
        // explicitly avoids a confusing NPE/ClassCastException on data
        // that LOOKS like a simple string mistake to the person who
        // uploaded it.
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row){
        for(Cell cell : row){
            if(cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private double getCellNumeric(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) throw new IllegalArgumentException("Missing numeric value in column " + (col + 1));
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> Double.parseDouble(cell.getStringCellValue().trim());
            default -> throw new IllegalArgumentException("Invalid numeric value in column " + (col + 1));
        };
    }

    public byte[] exportProductsToExcel() {
        List<Product> products = productRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Products");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"SKU", "Name", "Description", "Price", "Stock", "Category", "Tags", "Active"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getSku());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getDescription() != null ? p.getDescription() : "");
                row.createCell(3).setCellValue(p.getPrice().doubleValue());
                row.createCell(4).setCellValue(p.getStockQuantity());
                row.createCell(5).setCellValue(p.getCategory().getName());
                row.createCell(6).setCellValue(String.join(",", p.getTags().stream().map(Tag::getName).toList()));
                row.createCell(7).setCellValue(p.isActive());
            }

            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate export", e);
        }
    }


}
