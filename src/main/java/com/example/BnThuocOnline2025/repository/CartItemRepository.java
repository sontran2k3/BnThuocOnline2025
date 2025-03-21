package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.CartItem;
import com.example.BnThuocOnline2025.model.DonViTinh;
import com.example.BnThuocOnline2025.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    Optional<CartItem> findByCartAndProductAndDonViTinh(Cart cart, Product product, DonViTinh donViTinh);
    List<CartItem> findByCart(Cart cart);

}