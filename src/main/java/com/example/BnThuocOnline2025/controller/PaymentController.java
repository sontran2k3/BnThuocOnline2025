package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.model.CreatePaymentLinkRequestBody;
import com.example.BnThuocOnline2025.model.OrderItem;
import com.example.BnThuocOnline2025.model.Orders;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.repository.*;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/thanhtoan")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(GioHangService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private DonViTinhRepository donViTinhRepository;

    private final PayOS payOS;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;

    @Autowired
    public PaymentController(PayOS payOS, OrdersRepository ordersRepository, UserRepository userRepository) {
        this.payOS = payOS;
        this.ordersRepository = ordersRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/payos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> createPayOSPayment(
            @RequestBody Map<String, Object> orderData,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpServletRequest request,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Log dữ liệu nhận được
            logger.info("Received orderData: {}", orderData);

            // Kiểm tra dữ liệu đầu vào
            String customerName = (String) orderData.get("customerName");
            String customerPhone = (String) orderData.get("customerPhone");
            String customerAddress = (String) orderData.get("customerAddress");
            Number totalAmountNumber = (Number) orderData.get("totalAmount");
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");

            if (customerName == null || customerPhone == null || customerAddress == null ||
                    totalAmountNumber == null || items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Dữ liệu đơn hàng không đầy đủ: " +
                        "customerName=" + customerName + ", customerPhone=" + customerPhone +
                        ", customerAddress=" + customerAddress + ", totalAmount=" + totalAmountNumber +
                        ", items=" + (items == null ? "null" : items.size()));
            }

            int totalAmount = totalAmountNumber.intValue();
            if (totalAmount <= 0) {
                throw new IllegalArgumentException("Tổng tiền phải lớn hơn 0");
            }

            // Kiểm tra tổng tiền
            int calculatedTotal = items.stream()
                    .mapToInt(item -> {
                        Number quantity = (Number) item.get("quantity");
                        Number price = (Number) item.get("price");
                        if (quantity == null || price == null) {
                            throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ: quantity=" + quantity + ", price=" + price);
                        }
                        return quantity.intValue() * price.intValue();
                    })
                    .sum();
            if (calculatedTotal != totalAmount) {
                throw new IllegalArgumentException("Tổng tiền không khớp với danh sách sản phẩm: " +
                        "calculatedTotal=" + calculatedTotal + ", totalAmount=" + totalAmount);
            }

            // Lấy thông tin người dùng
            User user = null;
            if (oAuth2User != null) {
                String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
                String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
                user = "google".equals(provider) ? userRepository.findByGoogleId(providerId).orElse(null)
                        : userRepository.findByFacebookId(providerId).orElse(null);
                logger.info("OAuth2User provider: {}, providerId: {}, user: {}", provider, providerId, user != null ? user.getId() : "null");
            } else if (request.getUserPrincipal() != null) {
                String emailOrPhone = request.getUserPrincipal().getName();
                user = userRepository.findByEmail(emailOrPhone).orElse(null);
                if (user == null) {
                    user = userRepository.findByPhoneNumber(emailOrPhone).orElse(null);
                }
                logger.info("Principal name: {}, user: {}", emailOrPhone, user != null ? user.getId() : "null");
            } else {
                logger.info("No authenticated user found. Proceeding as guest.");
            }

            // Tạo đơn hàng
            Orders order = new Orders();
            order.setOrderDate(LocalDateTime.now());
            order.setTotalPrice(new BigDecimal(totalAmount));
            order.setStatus(Orders.OrderStatus.pending);
            order.setCustomerName(customerName);
            order.setCustomerPhone(customerPhone);
            order.setCustomerAddress(customerAddress);
            order.setPaymentMethod("payos");

            // Gán user_id hoặc để null cho khách vãng lai
            if (user != null) {
                order.setUserId(user.getId());
                order.setUser(user);
                logger.info("Assigned user ID {} to order", user.getId());
            } else {
                order.setUserId(null); // Khách vãng lai
                logger.info("Guest order created without user_id");
            }

            // Lưu đơn hàng
            try {
                order = ordersRepository.save(order);
                logger.info("Order saved with ID: {}, user_id: {}", order.getId(), order.getUserId());
            } catch (Exception e) {
                logger.error("Error saving order: {}", e.getMessage(), e);
                throw new IllegalStateException("Không thể lưu đơn hàng: " + e.getMessage());
            }

            // Lưu chi tiết đơn hàng
            for (Map<String, Object> item : items) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                Number productIdObj = (Number) item.get("productId");
                Number donViTinhIdObj = (Number) item.get("donViTinhId");
                if (productIdObj == null || donViTinhIdObj == null) {
                    throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ: productId=" + productIdObj + ", donViTinhId=" + donViTinhIdObj);
                }
                int productId = productIdObj.intValue();
                int donViTinhId = donViTinhIdObj.intValue();
                orderItem.setProduct(productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + productId)));
                orderItem.setDonViTinh(donViTinhRepository.findById(donViTinhId)
                        .orElseThrow(() -> new IllegalArgumentException("Đơn vị tính không tồn tại: " + donViTinhId)));
                Number quantityObj = (Number) item.get("quantity");
                Number priceObj = (Number) item.get("price");
                if (quantityObj == null || priceObj == null) {
                    throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ: quantity=" + quantityObj + ", price=" + priceObj);
                }
                orderItem.setQuantity(quantityObj.intValue());
                orderItem.setPrice(new BigDecimal(priceObj.doubleValue()));
                orderItem.setAddedAt(LocalDateTime.now());
                try {
                    orderItemRepository.save(orderItem);
                } catch (Exception e) {
                    logger.error("Error saving order item: {}", e.getMessage(), e);
                    throw new IllegalStateException("Không thể lưu chi tiết đơn hàng: " + e.getMessage());
                }
            }

            // Chuẩn bị dữ liệu thanh toán PayOS
            String baseUrl = getBaseUrl(request);
            String returnUrl = baseUrl + "/thanhtoan/success?orderCode=" + order.getId();
            String cancelUrl = baseUrl + "/thanhtoan/cancel";

            List<ItemData> itemList = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Number productIdObj = (Number) item.get("productId");
                Number quantityObj = (Number) item.get("quantity");
                Number priceObj = (Number) item.get("price");
                if (productIdObj == null || quantityObj == null || priceObj == null) {
                    throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ: productId=" + productIdObj +
                            ", quantity=" + quantityObj + ", price=" + priceObj);
                }
                String productName = "Sản phẩm ID: " + productIdObj.intValue();
                int quantity = quantityObj.intValue();
                int price = priceObj.intValue();
                itemList.add(ItemData.builder()
                        .name(productName)
                        .quantity(quantity)
                        .price(price)
                        .build());
            }

            String description = "ĐH #" + order.getId();
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            // Tạo orderCode duy nhất
            long orderCode = Long.parseLong(String.valueOf(order.getId()) + new Random().nextInt(1000));

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(totalAmount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .items(itemList)
                    .build();

            // Log dữ liệu PayOS
            logger.info("PayOS payment data: {}", paymentData);

            // Tạo link thanh toán
            CheckoutResponseData data;
            try {
                data = payOS.createPaymentLink(paymentData);
                logger.info("PayOS response: {}", data);
            } catch (Exception e) {
                logger.error("Error creating PayOS payment link: {}", e.getMessage(), e);
                throw new IllegalStateException("Lỗi từ PayOS: " + e.getMessage());
            }

            response.put("success", true);
            response.put("checkoutUrl", data.getCheckoutUrl());
            response.put("orderCode", order.getId());
        } catch (Exception e) {
            logger.error("Error creating payment link: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tạo link thanh toán: " +
                    (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
        return response;
    }

    // Trang thành công
    @GetMapping("/success")
    public String success(@RequestParam(value = "orderCode", required = false) String orderCode, Model model) {
        try {
            if (orderCode != null) {
                // Cập nhật trạng thái đơn hàng
                Orders order = ordersRepository.findById(Integer.parseInt(orderCode)).orElse(null);
                if (order != null) {
                    order.setStatus(Orders.OrderStatus.completed);
                    ordersRepository.save(order);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        model.addAttribute("page", "success");
        model.addAttribute("pageTitle", "Thanh toán thành công");
        return "payment";
    }

    // Trang hủy
    @GetMapping("/cancel")
    public String cancel(Model model) {
        model.addAttribute("page", "cancel");
        model.addAttribute("pageTitle", "Thanh toán thất bại");
        return "payment";
    }

    // Helper method: Lấy base URL
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String url = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url += ":" + serverPort;
        }
        url += contextPath;
        return url;
    }
}