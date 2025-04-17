package com.example.BnThuocOnline2025.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryResponseDTO {
    private Integer id;
    private KhoDTO kho;
    private ProductDTO product;
    private SupplierDTO supplier;
    private String batchNumber;
    private Integer quantity;
    private LocalDateTime manufactureDate;
    private LocalDateTime expiryDate;
    private String storageLocation;
    private String ghichu;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer supplierId;
    private Integer warehouseId;
    private Integer productId;
    private String note;

    @Data
    public static class KhoDTO {
        private Integer id;
        private String maKho;
        private String tenKho;
    }

    @Data
    public static class ProductDTO {
        private Integer id;
        private String tenSanPham;
    }

    @Data
    public static class SupplierDTO {
        private Integer id;
        private String tenNhaCungCap;
    }
}