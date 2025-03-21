package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {
    Optional<Orders> findById(Integer id);
//    List<Orders> findByUserId(UUID userId);


//    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.cart WHERE o.userId = :userId")
//    List<Orders> findByUserIdWithCart(UUID userId);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.cart WHERE o.user.id = :userId")
    List<Orders> findByUserIdWithCart(@Param("userId") UUID userId);

    // Tìm đơn hàng theo userId (không fetch cart)
    List<Orders> findByUserId(@Param("userId") UUID userId);

    // Tìm kiếm theo ID đơn hàng
    @Query("SELECT o FROM Orders o WHERE CAST(o.id AS string) LIKE %:search%")
    Page<Orders> findByIdContaining(@Param("search") String search, Pageable pageable);

    // Lọc theo trạng thái
    Page<Orders> findByStatus(@Param("status") String status, Pageable pageable);

    // Tìm kiếm và lọc theo trạng thái
    @Query("SELECT o FROM Orders o WHERE CAST(o.id AS string) LIKE %:search% AND o.status = :status")
    Page<Orders> findByIdContainingAndStatus(@Param("search") String search, @Param("status") String status, Pageable pageable);
}