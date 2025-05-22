package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {
    Optional<Orders> findById(Integer id);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "orderItems.product.productImages"})
    List<Orders> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.cart WHERE o.user.id = :userId")
    List<Orders> findByUserIdWithCart(@Param("userId") UUID userId);

    // Tìm kiếm theo ID đơn hàng
    @Query("SELECT o FROM Orders o WHERE CAST(o.id AS string) LIKE %:search%")
    Page<Orders> findByIdContaining(@Param("search") String search, Pageable pageable);

    // Lọc theo trạng thái
    Page<Orders> findByStatus(@Param("status") String status, Pageable pageable);

    // Tìm kiếm và lọc theo trạng thái
    @Query("SELECT o FROM Orders o WHERE CAST(o.id AS string) LIKE %:search% AND o.status = :status")
    Page<Orders> findByIdContainingAndStatus(@Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT DATE(o.order_date) AS ngay, " +
            "COALESCE(SUM(oi.price * oi.quantity), 0) AS doanh_thu " +
            "FROM orders o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE o.status = 'completed' " +
            "GROUP BY DATE(o.order_date) " +
            "ORDER BY DATE(o.order_date) DESC", nativeQuery = true)
    List<Map<String, Object>> findDailyRevenue();

    @Query(value = "SELECT YEAR(o.order_date) AS nam, " +
            "WEEK(o.order_date, 1) AS tuan, " +
            "MIN(DATE(o.order_date)) AS bat_dau_tuan, " +
            "COALESCE(SUM(o.total_price), 0) AS doanh_thu " +
            "FROM orders o " +
            "WHERE o.status = 'completed' " +
            "GROUP BY YEAR(o.order_date), WEEK(o.order_date, 1) " +
            "ORDER BY YEAR(o.order_date) DESC, WEEK(o.order_date, 1) DESC", nativeQuery = true)
    List<Map<String, Object>> findWeeklyRevenue();

    @Query(value = "SELECT YEAR(o.order_date) AS nam, " +
            "MONTH(o.order_date) AS thang, " +
            "CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) AS thang_nam, " +
            "COALESCE(SUM(o.total_price), 0) AS doanh_thu " +
            "FROM orders o " +
            "WHERE o.status = 'completed' " +
            "GROUP BY YEAR(o.order_date), MONTH(o.order_date), CONCAT(YEAR(o.order_date), '-', LPAD(MONTH(o.order_date), 2, '0')) " +
            "ORDER BY YEAR(o.order_date) DESC, MONTH(o.order_date) DESC", nativeQuery = true)
    List<Map<String, Object>> findMonthlyRevenue();

}