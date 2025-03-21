package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.NhaCungCap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NhaCungCapRepository extends JpaRepository<NhaCungCap, Integer> {
}