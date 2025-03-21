package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danhmuc")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class DanhMuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "ten_danh_muc", nullable = false)
    private String tenDanhMuc;

    @Column(name = "ghi_chu")
    private String ghiChu;
}