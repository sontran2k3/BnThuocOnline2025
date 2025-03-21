package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "chitietsanpham")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id") // Chỉ dùng id để tính hashCode và equals, loại trừ product
public class ChiTietSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(name = "mota_chi_tiet", columnDefinition = "TEXT")
    private String moTaChiTiet;

    @Column(name = "thanhphan_chi_tiet", columnDefinition = "TEXT")
    private String thanhPhanChiTiet;

    @Column(name = "cong_dung_chi_tiet", columnDefinition = "TEXT")
    private String congDungChiTiet;

    @Column(name = "cach_dung_chi_tiet", columnDefinition = "TEXT")
    private String cachDungChiTiet;

    @Column(name = "tac_dung_phu", columnDefinition = "TEXT")
    private String tacDungPhu;

    @Column(name = "luu_y", columnDefinition = "TEXT")
    private String luuY;

    @Column(name = "ban_quan", columnDefinition = "TEXT")
    private String baoQuan;

    @OneToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonBackReference(value = "product-chitiet")
    private Product product;
}