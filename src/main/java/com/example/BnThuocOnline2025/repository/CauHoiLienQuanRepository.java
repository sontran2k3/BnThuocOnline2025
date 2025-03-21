package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.CauHoiLienQuan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CauHoiLienQuanRepository extends JpaRepository<CauHoiLienQuan, Integer> {
    List<CauHoiLienQuan> findByProductId(int productId);
}