package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    List<Inventory> findByProductId(Integer productId);

    boolean existsByKhoIdAndBatchNumber(Integer khoId, String batchNumber);

    @Query("SELECT i FROM Inventory i " +
            "JOIN FETCH i.kho " +
            "JOIN FETCH i.product " +
            "LEFT JOIN FETCH i.supplier")
    List<Inventory> findAllWithDetails();
}