package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.dto.*;
import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.service.QuanLyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public ResponseEntity<List<InventoryResponseDTO>> getInventoryByProductId(@PathVariable Integer productId) {
        List<InventoryResponseDTO> inventories = quanLyService.getInventoryByProductIdDTO(productId);
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
    public ResponseEntity<?> addInventory(@RequestBody InventoryResponseDTO inventoryDTO) {
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
    public ResponseEntity<List<InventoryResponseDTO>> getAllInventory() {
        List<InventoryResponseDTO> inventoryList = quanLyService.getAllInventory();
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


    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = quanLyService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Lấy thông tin chi tiết người dùng
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            Optional<UserDTO> user = quanLyService.getUserById(id);
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
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserDTO userDTO) {
        try {
            UserDTO updatedUser = quanLyService.updateUser(id, userDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }


    // Lấy chi tiết đơn hàng
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer orderId) {
        try {
            Orders order = quanLyService.getOrderDetails(orderId);
            OrderDetailDTO orderDetailDTO = new OrderDetailDTO();
            orderDetailDTO.setId(order.getId());
            orderDetailDTO.setOrderDate(order.getOrderDate());
            orderDetailDTO.setTotalPrice(order.getTotalPrice());
            orderDetailDTO.setStatus(order.getStatus().toString());
            orderDetailDTO.setCustomerName(order.getCustomerName());
            orderDetailDTO.setCustomerPhone(order.getCustomerPhone());
            orderDetailDTO.setCustomerAddress(order.getCustomerAddress());
            orderDetailDTO.setPaymentMethod(order.getPaymentMethod());

            List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream().map(item -> {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductName(item.getProduct() != null ? item.getProduct().getTenSanPham() : "N/A");
                itemDTO.setProductImageUrl(item.getProduct() != null ? item.getProduct().getMainImageUrl() : "");
                itemDTO.setDonViTinh(item.getDonViTinh() != null ? item.getDonViTinh().getDonViTinh() : "N/A");
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                return itemDTO;
            }).collect(Collectors.toList());

            orderDetailDTO.setOrderItems(orderItemDTOs);
            return ResponseEntity.ok(orderDetailDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Lấy danh sách đơn hàng theo userId
    @GetMapping("/orders")
    public ResponseEntity<?> getOrdersByUserId(@RequestParam("user_id") UUID userId) {
        try {
            List<Orders> orders = quanLyService.getOrdersByUserId(userId);
            List<OrderDTO> orderDTOs = orders.stream().map(order -> {
                OrderDTO orderDTO = new OrderDTO();
                orderDTO.setId(order.getId());
                orderDTO.setOrderDate(order.getOrderDate());
                orderDTO.setTotalPrice(order.getTotalPrice());
                orderDTO.setStatus(order.getStatus().toString());

                List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream().map(item -> {
                    OrderItemDTO itemDTO = new OrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setProductName(item.getProduct() != null ? item.getProduct().getTenSanPham() : "N/A");
                    itemDTO.setProductImageUrl(item.getProduct() != null ? item.getProduct().getMainImageUrl() : "");
                    itemDTO.setDonViTinh(item.getDonViTinh() != null ? item.getDonViTinh().getDonViTinh() : "N/A");
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPrice(item.getPrice());
                    return itemDTO;
                }).collect(Collectors.toList());

                orderDTO.setOrderItems(orderItemDTOs);
                return orderDTO;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(orderDTOs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/inventory-transactions")
    public ResponseEntity<List<InventoryTransactionDTO>> getInventoryTransactions(
            @RequestParam(required = false) Integer khoId,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
            List<InventoryTransactionDTO> transactions = quanLyService.getInventoryTransactions(khoId, productId, start, end);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }




    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String search) {
        try {
            Page<ProductReview> reviewPage = quanLyService.getAllReviews(page, 10, search);
            List<ReviewDTO> reviewDTOs = reviewPage.getContent().stream().map(review -> {
                ReviewDTO dto = new ReviewDTO();
                dto.setId(review.getId());
                dto.setCreated_at(review.getCreatedAt());
                dto.setRating(review.getRating());
                dto.setReview_content(review.getReviewContent());
                dto.setUpdated_at(review.getUpdatedAt());
                dto.setProduct_id(review.getProduct() != null ? review.getProduct().getId() : 0);
                dto.setProduct_name(review.getProduct() != null ? review.getProduct().getTenSanPham() : "Không xác định");
                dto.setUser_id(review.getUser() != null ? review.getUser().getId() : null);
                dto.setUsername(review.getUser() != null ? review.getUser().getName() : "Không xác định");
                dto.setApproved(review.isApproved());
                dto.setReply(review.getReply());
                return dto;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviewDTOs);
            response.put("totalPages", reviewPage.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<String> approveReview(@PathVariable int reviewId) {
        try {
            quanLyService.approveReview(reviewId);
            return ResponseEntity.ok("Duyệt đánh giá thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<String> replyToReview(@PathVariable int reviewId, @RequestBody Map<String, String> request) {
        try {
            String replyContent = request.get("replyContent");
            if (replyContent == null || replyContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Nội dung phản hồi không được để trống!");
            }
            quanLyService.replyToReview(reviewId, replyContent);
            return ResponseEntity.ok("Gửi phản hồi thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}