package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {
    Optional<ChiTietSanPham> findByProductId(int productId);
}