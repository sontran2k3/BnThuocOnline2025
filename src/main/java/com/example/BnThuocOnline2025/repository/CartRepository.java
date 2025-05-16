package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(User user);
    Optional<Cart> findBySessionId(String sessionId);
    List<Cart> findAllByUser(User user);

    // Thêm phương thức mới để lấy tất cả giỏ hàng theo sessionId
    List<Cart> findAllBySessionId(String sessionId);
}