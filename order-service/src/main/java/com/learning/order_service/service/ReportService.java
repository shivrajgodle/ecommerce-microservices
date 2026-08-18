package com.learning.order_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import com.learning.order_service.dto.response.SalesReportResponse;
import com.learning.order_service.dto.response.StatusStat;
import com.learning.order_service.entity.Order;
import com.learning.order_service.entity.OrderStatus;
import com.learning.order_service.repository.OrderRepository;


import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ReportService {

     private final OrderRepository orderRepository;

     public SalesReportResponse generateSalesReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<StatusStat> breakdown = orderRepository.getStatusBreakdown(start, end);

        long totalOrders = breakdown.stream().mapToLong(StatusStat::getCount).sum();
        BigDecimal totalRevenue = breakdown.stream()
                .filter(s -> s.getStatus() == OrderStatus.CONFIRMED)
                .map(StatusStat::getTotalAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        return SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .statusBreakdown(breakdown)
                .build();
    }


     public byte[] exportSalesReportToExcel(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        SalesReportResponse summary = generateSalesReport(startDate, endDate);
        List<Order> orders = orderRepository.findByCreatedDateBetweenOrderByCreatedDateAsc(start, end);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // SHEET 1 — the summary a manager actually wants to glance
            // at first. Kept SEPARATE from the raw order list rather
            // than mixed into one sheet — a report combining "here's
            // the headline number" with "here's 4,000 raw rows" on the
            // same sheet forces the reader to scroll past the detail to
            // find the number they actually opened the file for.
            Sheet summarySheet = workbook.createSheet("Summary");
            int r = 0;
            summarySheet.createRow(r++).createCell(0).setCellValue("Sales Report: " + startDate + " to " + endDate);
            summarySheet.createRow(r++);
            Row totalOrdersRow = summarySheet.createRow(r++);
            totalOrdersRow.createCell(0).setCellValue("Total Orders");
            totalOrdersRow.createCell(1).setCellValue(summary.getTotalOrders());
            Row revenueRow = summarySheet.createRow(r++);
            revenueRow.createCell(0).setCellValue("Total Revenue (Confirmed Orders Only)");
            revenueRow.createCell(1).setCellValue(summary.getTotalRevenue().doubleValue());
            r++;

            Row breakdownHeader = summarySheet.createRow(r++);
            breakdownHeader.createCell(0).setCellValue("Status");
            breakdownHeader.createCell(1).setCellValue("Count");
            breakdownHeader.createCell(2).setCellValue("Amount");
            for (Cell c : breakdownHeader) c.setCellStyle(headerStyle);

            for (StatusStat stat : summary.getStatusBreakdown()) {
                Row row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue(stat.getStatus().name());
                row.createCell(1).setCellValue(stat.getCount());
                row.createCell(2).setCellValue(stat.getTotalAmount().doubleValue());
            }
            for (int i = 0; i < 3; i++) summarySheet.autoSizeColumn(i);

            // SHEET 2 — the full line-item detail, for anyone who needs
            // to drill in past the summary.
            Sheet detailSheet = workbook.createSheet("Orders");
            Row detailHeader = detailSheet.createRow(0);
            String[] cols = {"Order ID", "Date", "Status", "Total Amount"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = detailHeader.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int rowNum = 1;
            for (Order order : orders) {
                Row row = detailSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getId());
                row.createCell(1).setCellValue(order.getCreatedDate().format(fmt));
                row.createCell(2).setCellValue(order.getStatus().name());
                row.createCell(3).setCellValue(order.getTotalAmount().doubleValue());
            }
            for (int i = 0; i < cols.length; i++) detailSheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate sales report", e);
        }
    }
}
