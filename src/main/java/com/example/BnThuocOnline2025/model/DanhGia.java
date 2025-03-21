package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "danhgia")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private byte[] userId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Quan hệ ManyToOne với User (nếu bảng user tồn tại)
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    // Quan hệ ManyToOne với Product
    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Product product;

    // Constructor không bao gồm user và product (dùng khi không cần quan hệ)
    public DanhGia(int id, byte[] userId, int productId, int rating, String reviewText, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.reviewText = reviewText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Phương thức để kiểm tra rating hợp lệ (1-5)
    @PrePersist
    @PreUpdate
    private void validateRating() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating phải nằm trong khoảng từ 1 đến 5");
        }
    }
}