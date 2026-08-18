package com.learning.order_service.controller;

import com.learning.order_service.client.dto.request.CheckoutRequest;
import com.learning.order_service.client.dto.response.OrderDetailResponse;
import com.learning.order_service.client.dto.response.OrderResponse;
import com.learning.order_service.dto.response.ApiResponse;
import com.learning.order_service.dto.response.SalesReportResponse;
import com.learning.order_service.exception.ForbiddenOperationException;
import com.learning.order_service.security.CurrentUserId;
import com.learning.order_service.service.OrderService;
import com.learning.order_service.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.SortOrder;

import org.hibernate.query.SortDirection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReportService reportService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@CurrentUserId Long userId, @Valid @RequestBody CheckoutRequest request){
        OrderResponse result = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(), "Order Created , awaiting payment",result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @CurrentUserId Long userId, @PathVariable Long id) {
        OrderDetailResponse result = orderService.getOrderDetail(userId, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Order retrieved", result));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @CurrentUserId Long userId,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersForUser(userId, pageable));
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestHeader(value = "X-Auth-User-Roles", required = false) String rolesHeader,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        requireAdmin(rolesHeader);
        SalesReportResponse result = reportService.generateSalesReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Sales report generated", result));
    }

    @GetMapping("/reports/sales/export")
    public ResponseEntity<ByteArrayResource> exportSalesReport(@RequestHeader(value = "X-Auth-User-Roles", required = false) String rolesHeader,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) 
    {
        requireAdmin(rolesHeader);
        byte[] data = reportService.exportSalesReportToExcel(startDate, endDate);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=sales-report-" + startDate + "-to-" + endDate + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(resource);
    }


    /**
     * Same reasoning as Review Service's Phase J deleteReview() — a
     * plain, explicit check rather than a framework annotation, because
     * it's a single condition on a header value, not a broader
     * declarative rule shared across many endpoints. Now used a SECOND
     * time (Review Service was the first) — per the "duplicate twice
     * before generalizing" instinct from that same phase, this is right
     * at the point where a THIRD use would be the signal to extract a
     * reusable @RequireAdmin-style mechanism instead.
     */
    private void requireAdmin(String rolesHeader) {
        List<String> roles = rolesHeader != null ? Arrays.asList(rolesHeader.split(",")) : Collections.emptyList();
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenOperationException("This endpoint requires admin privileges");
        }
    }

}
