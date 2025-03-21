package com.example.BnThuocOnline2025.converter;

import com.example.BnThuocOnline2025.model.Kho;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoaiKhoConverter implements AttributeConverter<Kho.LoaiKho, String> {

    @Override
    public String convertToDatabaseColumn(Kho.LoaiKho loaiKho) {
        return switch (loaiKho) {
            case THƯỜNG -> "THƯỜNG";
            case LẠNH -> "LẠNH";
            case HÓA_CHẤT -> "HÓA CHẤT";
        };
    }

    @Override
    public Kho.LoaiKho convertToEntityAttribute(String dbData) {
        return switch (dbData.toUpperCase()) {
            case "THƯỜNG" -> Kho.LoaiKho.THƯỜNG;
            case "LẠNH" -> Kho.LoaiKho.LẠNH;
            case "HÓA CHẤT" -> Kho.LoaiKho.HÓA_CHẤT;
            default -> throw new IllegalArgumentException("Unknown value: " + dbData);
        };
    }
}