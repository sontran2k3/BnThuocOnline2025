package com.example.BnThuocOnline2025.converter;

import com.example.BnThuocOnline2025.model.NhaCungCap;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoaiNhaCungCapConverter implements AttributeConverter<NhaCungCap.LoaiNhaCungCap, String> {

    @Override
    public String convertToDatabaseColumn(NhaCungCap.LoaiNhaCungCap attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case NOI_DIA -> "NỘI ĐỊA";
            case QUOC_TE -> "QUỐC TẾ";
        };
    }

    @Override
    public NhaCungCap.LoaiNhaCungCap convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "NỘI ĐỊA" -> NhaCungCap.LoaiNhaCungCap.NOI_DIA;
            case "QUỐC TẾ" -> NhaCungCap.LoaiNhaCungCap.QUOC_TE;
            default -> throw new IllegalArgumentException("Unknown LoaiNhaCungCap value: " + dbData);
        };
    }
}