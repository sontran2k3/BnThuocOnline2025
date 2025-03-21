package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user); // Giữ lại để tương thích với mã cũ nếu cần
    Optional<Cart> findBySessionId(String sessionId);
    List<Cart> findAllByUser(User user); // Thêm để lấy tất cả giỏ hàng của user


}