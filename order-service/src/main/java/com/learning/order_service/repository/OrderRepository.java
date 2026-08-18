package com.learning.order_service.repository;

import com.learning.order_service.dto.response.StatusStat;
import com.learning.order_service.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserId(Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT new com.learning.order_service.dto.response.StatusStat(o.status, COUNT(o), COALESCE(SUM(o.totalAmount), 0)) " +
       "FROM Order o WHERE o.createdDate BETWEEN :start AND :end GROUP BY o.status")
    List<StatusStat> getStatusBreakdown(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Order> findByCreatedDateBetweenOrderByCreatedDateAsc(LocalDateTime start, LocalDateTime end);
}
