package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "don_vi_tinh_id")
    private DonViTinh donViTinh;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Transient // Không lưu vào DB, chỉ dùng tạm thời
    private BigDecimal itemTotalPrice;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}