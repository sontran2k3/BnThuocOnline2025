package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByDanhMucId(int danhMucId);
    List<Product> findByDoiTuongId(int doiTuongId);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.danhMuc " +
            "LEFT JOIN FETCH p.doiTuong " +
            "LEFT JOIN FETCH p.donViTinhList")
    List<Product> findAllWithDetails();


    List<Product> findByTenSanPhamContainingIgnoreCase(String tenSanPham);


}