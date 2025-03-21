package com.example.BnThuocOnline2025.model;

import com.example.BnThuocOnline2025.converter.LoaiKhoConverter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kho")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class Kho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_kho", nullable = false, unique = true, length = 20)
    private String maKho;

    @Column(name = "ten_kho", nullable = false, length = 100)
    private String tenKho;

    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    @Column(name = "dien_tich", precision = 10, scale = 2)
    private BigDecimal dienTich;

    @Column(name = "suc_chua")
    private Integer sucChua;

    @Convert(converter = LoaiKhoConverter.class)
    @Column(name = "loai_kho", nullable = false)
    private LoaiKho loaiKho = LoaiKho.THƯỜNG;



    @Column(name = "nguoi_quanly", length = 50)
    private String nguoiQuanly;

    @Column(name = "so_dien_thoai_lien_he", length = 20)
    private String soDienThoaiLienHe;

    @Column(name = "email_lien_he", length = 100)
    private String emailLienHe;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai = TrangThai.ACTIVE;

    @Column(name = "ngay_khoi_tao")
    private LocalDate ngayKhoiTao;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    private User updatedBy;

    public enum LoaiKho {
        THƯỜNG, LẠNH, HÓA_CHẤT
    }

    public enum TrangThai {
        ACTIVE, INACTIVE, MAINTENANCE
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}