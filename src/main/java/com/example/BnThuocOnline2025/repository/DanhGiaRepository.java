package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {
    long countByCreatedAtAfter(LocalDateTime dateTime);
}