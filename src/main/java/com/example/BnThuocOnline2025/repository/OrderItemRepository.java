package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.OrderItem;
import com.example.BnThuocOnline2025.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder(Orders order);
}