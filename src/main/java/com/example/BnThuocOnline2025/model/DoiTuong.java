package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doituong")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class DoiTuong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "image_max", nullable = false)
    private String imageMax; // Ảnh lớn cho desktop

    @Column(name = "image_min", nullable = false)
    private String imageMin; // Ảnh nhỏ cho mobile

    @Column(name = "ten_doi_tuong", nullable = false)
    private String tenDoiTuong;

    @Column(name = "ghi_chu")
    private String ghiChu;
}