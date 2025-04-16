package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.dto.InventoryDTO;
import com.example.BnThuocOnline2025.dto.ProductDTO;
import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.service.QuanLyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quanly")
public class QuanLyController {

    @Autowired
    private QuanLyService quanLyService;

    @GetMapping("/danhmuc")
    public ResponseEntity<List<DanhMuc>> getAllDanhMuc() {
        List<DanhMuc> danhMucList = quanLyService.getAllDanhMuc();
        return ResponseEntity.ok(danhMucList);
    }

    @PostMapping("/danhmuc")
    public ResponseEntity<DanhMuc> addDanhMuc(@RequestBody DanhMuc danhMuc) {
        DanhMuc savedDanhMuc = quanLyService.addDanhMuc(danhMuc);
        return ResponseEntity.ok(savedDanhMuc);
    }

    // Đối tượng sử dụng endpoints
    @GetMapping("/doituong")
    public ResponseEntity<List<DoiTuong>> getAllDoiTuong() {
        List<DoiTuong> doiTuongList = quanLyService.getAllDoiTuong();
        return ResponseEntity.ok(doiTuongList);
    }

    @PostMapping("/doituong")
    public ResponseEntity<DoiTuong> addDoiTuong(@RequestBody DoiTuong doiTuong) {
        DoiTuong savedDoiTuong = quanLyService.addDoiTuong(doiTuong);
        return ResponseEntity.ok(savedDoiTuong);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = quanLyService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<?> getProductDetail(@PathVariable Integer productId) {
        try {
            ProductDTO productDetail = quanLyService.getProductDetail(productId);
            return ResponseEntity.ok(productDetail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Integer productId) {
        try {
            quanLyService.deleteProduct(productId);
            return ResponseEntity.ok("Xóa sản phẩm thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi khi xóa sản phẩm: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestBody ProductDTO productDTO) {
        try {
            Product savedProduct = quanLyService.addProduct(productDTO);
            return ResponseEntity.ok(savedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody ProductDTO productDTO) {
        try {
            ProductDTO updatedProduct = quanLyService.updateProduct(id, productDTO);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/inventory-by-product/{productId}")
    public ResponseEntity<List<Inventory>> getInventoryByProductId(@PathVariable Integer productId) {
        List<Inventory> inventories = quanLyService.getInventoryByProductId(productId);
        return ResponseEntity.ok(inventories);
    }

    @GetMapping("/kho")
    public ResponseEntity<List<Kho>> getAllKho() {
        List<Kho> khoList = quanLyService.getAllKho();
        return ResponseEntity.ok(khoList);
    }

    @PostMapping("/kho")
    public ResponseEntity<?> addKho(@RequestBody Kho kho) {
        try {
            Kho savedKho = quanLyService.addKho(kho);
            return ResponseEntity.ok(savedKho);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/nhacungcap")
    public ResponseEntity<List<NhaCungCap>> getAllNhaCungCap() {
        List<NhaCungCap> nhaCungCapList = quanLyService.getAllNhaCungCap();
        return ResponseEntity.ok(nhaCungCapList);
    }

    @PostMapping("/inventory")
    public ResponseEntity<?> addInventory(@RequestBody InventoryDTO inventoryDTO) {
        try {
            Inventory savedInventory = quanLyService.addInventory(inventoryDTO);
            return ResponseEntity.ok(savedInventory);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<Inventory>> getAllInventory() {
        List<Inventory> inventoryList = quanLyService.getAllInventory();
        return ResponseEntity.ok(inventoryList);
    }

    // Dashboard: Tổng quan
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Long>> getDashboardSummary() {
        Map<String, Long> summary = new HashMap<>();
        summary.put("totalProducts", quanLyService.getTotalProducts());
        summary.put("totalOrders", quanLyService.getTotalOrders());
        summary.put("totalUsers", quanLyService.getTotalUsers());
        summary.put("newReviews", quanLyService.getNewReviews());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/order-stats")
    public ResponseEntity<Map<String, Long>> getOrderStats() {
        return ResponseEntity.ok(quanLyService.getOrderStats());
    }

    @GetMapping("/dashboard/product-category-stats")
    public ResponseEntity<Map<String, Long>> getProductByCategoryStats() {
        return ResponseEntity.ok(quanLyService.getProductByCategoryStats());
    }
    // Lấy danh sách người dùng
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = quanLyService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Lấy thông tin chi tiết người dùng
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            Optional<User> user = quanLyService.getUserById(id);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(404).body("Người dùng không tồn tại!");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Xóa người dùng
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        try {
            quanLyService.deleteUser(id);
            return ResponseEntity.ok("Xóa người dùng thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Cập nhật thông tin người dùng
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody User user) {
        try {
            User updatedUser = quanLyService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }



    // Lấy danh sách đơn hàng theo userId
    @GetMapping("/orders")
    public ResponseEntity<List<Orders>> getOrdersByUserId(@RequestParam("user_id") UUID userId) {
        try {
            List<Orders> orders = quanLyService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Lấy chi tiết đơn hàng theo ID
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable int orderId) {
        try {
            Orders order = quanLyService.getOrderDetails(orderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Lấy tất cả đơn hàng với phân trang, tìm kiếm và lọc
    @GetMapping("/orders/all")
    public ResponseEntity<List<Orders>> getAllOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status) {
        try {
            List<Orders> orders = quanLyService.getAllOrders(page, size, search, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/orders/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable int orderId) {
        try {
            List<OrderItem> items = quanLyService.getOrderItems(orderId);
            List<Map<String, Object>> itemDTOs = items.stream().map(item -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", item.getId());
                dto.put("productName", item.getProduct().getTenSanPham());
                dto.put("donViTinh", item.getDonViTinh() != null ? item.getDonViTinh().getDonViTinh() : "N/A");
                dto.put("quantity", item.getQuantity());
                dto.put("price", item.getPrice());
                dto.put("totalPrice", item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                return dto;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(itemDTOs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }



}