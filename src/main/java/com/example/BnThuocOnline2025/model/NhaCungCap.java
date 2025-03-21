package com.example.BnThuocOnline2025.model;

import com.example.BnThuocOnline2025.converter.TrangThaiConverter;
import com.example.BnThuocOnline2025.converter.LoaiNhaCungCapConverter;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "nhacungcap")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NhaCungCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String tenNhaCungCap;

    @Column(unique = true, length = 20)
    private String maNhaCungCap;

    @Column(length = 100)
    private String nguoiDaiDien;

    @Column(length = 20)
    private String soDienThoai;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String diaChi;

    @Column(unique = true, length = 20)
    private String taxCode;

    @Column(length = 100)
    private String website;

    @Convert(converter = LoaiNhaCungCapConverter.class)
    @Column(columnDefinition = "ENUM('NỘI ĐỊA', 'QUỐC TẾ')")
    private LoaiNhaCungCap loaiNhaCungCap = LoaiNhaCungCap.NOI_DIA;

    @Convert(converter = TrangThaiConverter.class)
    @Column(columnDefinition = "ENUM('HOẠT ĐỘNG', 'KHÔNG HOẠT ĐỘNG')")
    private TrangThai trangThai = TrangThai.HOAT_DONG;

    @Column
    private LocalDate ngayHopTac;

    @Column(columnDefinition = "TEXT")
    private String ghiChu;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    private User updatedBy;

    public enum LoaiNhaCungCap {
        NOI_DIA, QUOC_TE
    }

    public enum TrangThai {
        HOAT_DONG, KHONG_HOAT_DONG
    }

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