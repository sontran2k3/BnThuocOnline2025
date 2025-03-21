package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@Setter
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference(value = "product-inventory")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "kho_id", nullable = false)
    @JsonBackReference(value = "kho-inventory")
    private Kho kho;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "manufacture_date")
    private LocalDateTime manufactureDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "storage_location")
    private String storageLocation;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    @JsonBackReference(value = "supplier-inventory")
    private NhaCungCap supplier;

    @Column(name = "ghichu")
    private String ghichu;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}