package com.learning.catalog_service.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * DELIBERATELY not "all rows succeeded or the whole request fails."
 * A real bulk upload from a real spreadsheet a real person edited by
 * hand WILL have some bad rows — a typo'd category name, a missing
 * price, a duplicate SKU with conflicting data. Failing the entire
 * 500-row upload because row 347 has a typo is a genuinely bad user
 * experience; the person has to find that one row, fix it, and
 * re-upload everything, including the 499 rows that were already fine.
 * This result object reports BOTH what succeeded and what didn't,
 * row by row, so the person can fix just the failures and re-upload
 * a trimmed-down file with only those rows.
 */
@Getter
@Builder
public class BulkUploadResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<RowError> errors;
}
