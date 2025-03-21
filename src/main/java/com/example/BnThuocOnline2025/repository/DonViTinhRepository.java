package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.DonViTinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DonViTinhRepository extends JpaRepository<DonViTinh, Integer> {
    List<DonViTinh> findByProductId(int productId);

    @Query("SELECT dvt FROM DonViTinh dvt WHERE dvt.product.id = :productId ORDER BY dvt.gia ASC")
    List<DonViTinh> findByProductIdOrderByGiaAsc(Integer productId);
}