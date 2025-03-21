package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.math.BigDecimal;

@Entity
@Table(name = "don_vi_tinh")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class DonViTinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference(value = "product-donvitinh")
    private Product product;

    @Column(name = "don_vi_tinh")
    private String donViTinh;

    @Column(name = "gia", nullable = false)
    private BigDecimal gia;

    @Column(name = "discount")
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "ghi_chu")
    private String ghiChu;

    public boolean hasDiscount() {
        return discount != null && discount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getDiscountedPrice() {
        BigDecimal effectiveDiscount = discount != null ? discount : BigDecimal.ZERO;
        if (hasDiscount()) {
            return gia.multiply(BigDecimal.ONE.subtract(effectiveDiscount.divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP)));
        }
        return gia;
    }
}