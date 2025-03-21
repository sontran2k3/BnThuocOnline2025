package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}