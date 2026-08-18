package com.learning.order_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesReportResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalOrders;
    /**
     * DELIBERATELY only sums CONFIRMED orders, not PENDING or
     * CANCELLED — "revenue" should mean money actually collected. A
     * PENDING order might still resolve to CANCELLED (Phase I's
     * saga), so counting it as revenue now would overstate real
     * income; a CANCELLED order was never charged at all. This is a
     * direct, visible consequence of the choreographed saga's
     * eventual consistency (Phase G-I) showing up in a business
     * report — "how much did we make" isn't answerable from a single
     * synchronous number, it has to respect which orders actually
     * reached a final, successful state.
     */
    private BigDecimal totalRevenue;
    private List<StatusStat> statusBreakdown;
}