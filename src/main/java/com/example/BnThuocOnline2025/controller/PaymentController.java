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
            // Lấy thông tin từ orderData
            String customerName = (String) orderData.get("customerName");
            String customerPhone = (String) orderData.get("customerPhone");
            String customerAddress = (String) orderData.get("customerAddress");
            int totalAmount = ((Number) orderData.get("totalAmount")).intValue();
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");

            // Tạo mã đơn hàng
            String currentTimeString = String.valueOf(new Date().getTime());
            long orderCode = Long.parseLong(currentTimeString.substring(currentTimeString.length() - 6));

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
                logger.warn("No authenticated user found. Proceeding as guest.");
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

            // Gán user_id nếu người dùng tồn tại
            if (user != null) {
                order.setUserId(user.getId());
                order.setUser(user);
                logger.info("Assigned user ID {} to order {}", user.getId(), orderCode);
            } else {
                logger.info("Order {} created without user_id (guest)", orderCode);
                // Nếu cột user_id không cho phép null, cần xử lý tại đây
                order.setUserId(null); // Hoặc tạo một UUID mặc định nếu cần
            }

            // Lưu đơn hàng
            order = ordersRepository.save(order);
            logger.info("Order saved with ID: {}, user_id: {}", order.getId(), order.getUserId());

            // Lưu chi tiết đơn hàng
            for (Map<String, Object> item : items) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(productRepository.findById(((Number) item.get("productId")).intValue()).orElse(null));
                orderItem.setDonViTinh(donViTinhRepository.findById(((Number) item.get("donViTinhId")).intValue()).orElse(null));
                orderItem.setQuantity(((Number) item.get("quantity")).intValue());
                orderItem.setPrice(new BigDecimal(((Number) item.get("price")).doubleValue()));
                orderItem.setAddedAt(LocalDateTime.now());
                orderItemRepository.save(orderItem);
            }

            // Chuẩn bị dữ liệu thanh toán PayOS
            String baseUrl = getBaseUrl(request);
            String returnUrl = baseUrl + "/thanhtoan/success?orderCode=" + order.getId();
            String cancelUrl = baseUrl + "/thanhtoan/cancel";

            List<ItemData> itemList = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String productName = "Sản phẩm ID: " + item.get("productId");
                int quantity = ((Number) item.get("quantity")).intValue();
                int price = ((Number) item.get("price")).intValue();
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

            PaymentData paymentData = PaymentData.builder()
                    .orderCode((long) order.getId())
                    .amount(totalAmount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .items(itemList)
                    .build();

            // Tạo link thanh toán
            CheckoutResponseData data = payOS.createPaymentLink(paymentData);

            response.put("success", true);
            response.put("checkoutUrl", data.getCheckoutUrl());
            response.put("orderCode", order.getId());
        } catch (Exception e) {
            logger.error("Error creating payment link: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Không thể tạo link thanh toán: " + e.getMessage());
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