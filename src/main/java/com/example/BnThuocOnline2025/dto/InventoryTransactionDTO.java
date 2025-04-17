package com.example.BnThuocOnline2025.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryTransactionDTO {
    private Integer id;
    private Integer inventoryId;
    private InventoryResponseDTO.KhoDTO kho;
    private InventoryResponseDTO.ProductDTO product;
    private InventoryResponseDTO.SupplierDTO supplier;
    private String batchNumber;
    private String transactionType;
    private Integer quantity;
    private LocalDateTime transactionDate;
    private String reason;
    private Integer orderId;
    private String createdBy;
    private LocalDateTime createdAt;
}