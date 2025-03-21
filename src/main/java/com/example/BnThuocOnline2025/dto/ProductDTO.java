package com.example.BnThuocOnline2025.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private int id;
    private String tenSanPham;
    private String thuongHieu;
    private String congDung;
    private int doiTuongId;
    private int danhMucId;
    private String dangBaoChe;
    private String quyCach;
    private String xuatXuThuongHieu;
    private String nhaSanXuat;
    private String nuocSanXuat;
    private String thanhPhan;
    private String moTaNgan;
    private String soDangKy;
    private int soLuong;
    private String mainImageUrl;
    private BigDecimal lowestPrice;
    private String donViTinh;
    private String manufactureDate;
    private String expiryDate;
    private List<ImageDTO> images;
    private List<DonViTinhDTO> donViTinhList;
    private ChiTietSanPhamDTO chiTietSanPham;

    @Data
    public static class ImageDTO {
        private String imageUrl;
        private boolean isMain;
        private int imageOrder;
    }

    @Data
    public static class DonViTinhDTO {
        private String donViTinh;
        private double gia;
        private Double discount; // Có thể null
        private String ghiChu;
    }

    @Data
    public static class ChiTietSanPhamDTO {
        private String moTaChiTiet;
        private String thanhPhanChiTiet;
        private String congDungChiTiet;
        private String cachDungChiTiet;
        private String tacDungPhu;
        private String luuY;
        private String baoQuan;
    }
}