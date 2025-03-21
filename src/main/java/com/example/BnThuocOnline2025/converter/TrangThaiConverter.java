package com.example.BnThuocOnline2025.converter;

import com.example.BnThuocOnline2025.model.NhaCungCap;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TrangThaiConverter implements AttributeConverter<NhaCungCap.TrangThai, String> {

    @Override
    public String convertToDatabaseColumn(NhaCungCap.TrangThai attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case HOAT_DONG -> "HOẠT ĐỘNG";
            case KHONG_HOAT_DONG -> "KHÔNG HOẠT ĐỘNG";
        };
    }

    @Override
    public NhaCungCap.TrangThai convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "HOẠT ĐỘNG" -> NhaCungCap.TrangThai.HOAT_DONG;
            case "KHÔNG HOẠT ĐỘNG" -> NhaCungCap.TrangThai.KHONG_HOAT_DONG;
            default -> throw new IllegalArgumentException("Unknown TrangThai value: " + dbData);
        };
    }
}