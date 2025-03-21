package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id") // Chỉ dùng id để tính hashCode và equals, loại trừ chiTietSanPham
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;

    @Column(name = "thuong_hieu")
    private String thuongHieu;

    @Column(name = "cong_dung")
    private String congDung;

    @ManyToOne
    @JoinColumn(name = "doi_tuong_id")
    private DoiTuong doiTuong;

    @ManyToOne
    @JoinColumn(name = "danhmuc_id")
    private DanhMuc danhMuc;

    @Column(name = "dang_bao_che")
    private String dangBaoChe;

    @Column(name = "quy_cach")
    private String quyCach;

    @Column(name = "xuat_xu_thuong_hieu")
    private String xuatXuThuongHieu;

    @Column(name = "nha_san_xuat")
    private String nhaSanXuat;

    @Column(name = "nuoc_san_xuat")
    private String nuocSanXuat;

    @Column(name = "thanh_phan")
    private String thanhPhan;

    @Column(name = "mo_ta_ngan")
    private String moTaNgan;

    @Column(name = "so_dang_ky")
    private String soDangKy;

    @Column(name = "so_luong")
    private int soLuong;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "product-donvitinh")
    private List<DonViTinh> donViTinhList;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "product-images")
    private List<ProductImage> productImages;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "product-chitiet")
    private ChiTietSanPham chiTietSanPham;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "product-inventory")
    private List<Inventory> inventories;

    @Transient
    private String mainImageUrl;

    public DonViTinh getDefaultDonViTinh() {
        if (donViTinhList == null || donViTinhList.isEmpty()) {
            DonViTinh defaultDvt = new DonViTinh();
            defaultDvt.setGia(BigDecimal.ZERO);
            defaultDvt.setDiscount(BigDecimal.ZERO);
            defaultDvt.setDonViTinh("N/A");
            return defaultDvt;
        }
        return donViTinhList.get(0);
    }

    public BigDecimal getDiscountedPrice() {
        DonViTinh defaultDvt = getDefaultDonViTinh();
        BigDecimal gia = defaultDvt.getGia();
        BigDecimal discount = defaultDvt.getDiscount() != null ? defaultDvt.getDiscount() : BigDecimal.ZERO;
        if (defaultDvt.hasDiscount()) {
            return gia.multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP)));
        }
        return gia;
    }

    public boolean hasDiscount() {
        return getDefaultDonViTinh().hasDiscount();
    }

    public BigDecimal getGia() {
        return getDefaultDonViTinh().getGia();
    }
}