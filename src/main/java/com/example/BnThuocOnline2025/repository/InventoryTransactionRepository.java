package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    @Query("SELECT t FROM InventoryTransaction t " +
            "JOIN FETCH t.kho " +
            "JOIN FETCH t.product " +
            "LEFT JOIN FETCH t.supplier " +
            "WHERE (:khoId IS NULL OR t.kho.id = :khoId) " +
            "AND (:productId IS NULL OR t.product.id = :productId) " +
            "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
            "ORDER BY t.transactionDate DESC")
    List<InventoryTransaction> findTransactions(
            @Param("khoId") Integer khoId,
            @Param("productId") Integer productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}