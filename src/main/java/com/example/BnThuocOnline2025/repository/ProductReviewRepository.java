package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Integer> {
    List<ProductReview> findByProductId(int productId);

    // Lọc đánh giá theo số sao
    List<ProductReview> findByProductIdAndRating(int productId, float rating);

    // Tính trung bình số sao
    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") int productId);

    // Đếm số lượng đánh giá
    long countByProductId(int productId);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    // Tìm kiếm đánh giá theo nội dung, không phân biệt hoa thường, với phân trang
    Page<ProductReview> findByReviewContentContainingIgnoreCase(String reviewContent, Pageable pageable);
}