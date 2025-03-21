package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Kho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhoRepository extends JpaRepository<Kho, Integer> {
    boolean existsByMaKho(String maKho);
}