package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProductId(int productId);

    Optional<ProductImage> findByProductIdAndIsMainTrue(int productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isMain = true")
    Optional<ProductImage> findMainImageByProductId(Integer productId);

}