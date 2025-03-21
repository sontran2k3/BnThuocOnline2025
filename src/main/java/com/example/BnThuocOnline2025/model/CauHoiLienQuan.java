package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cauhoilienquan")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class CauHoiLienQuan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(name = "cau_hoi", nullable = false, columnDefinition = "TEXT")
    private String cauHoi;

    @Column(name = "cau_tra_loi", nullable = false, columnDefinition = "TEXT")
    private String cauTraLoi;

    @ManyToOne
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}