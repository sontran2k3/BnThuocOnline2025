package com.example.BnThuocOnline2025.dto;

import com.example.BnThuocOnline2025.model.CartItem;

import java.math.BigDecimal;

public class CartItemDTO {
    private int id;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private String donViTinh;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDonViTinh() {
        return donViTinh;
    }

    public void setDonViTinh(String donViTinh) {
        this.donViTinh = donViTinh;
    }

    // Constructor, getters, setters
    public CartItemDTO(CartItem cartItem) {
        this.id = cartItem.getId();
        this.productName = cartItem.getProduct().getTenSanPham();
        this.price = cartItem.getPrice();
        this.quantity = cartItem.getQuantity();
        this.donViTinh = cartItem.getDonViTinh().getDonViTinh();
    }

    public CartItemDTO() {
    }
}
