package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.dto.CartItemDTO;
import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.repository.*;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
public class GioHangController {

    private static final Logger logger = LoggerFactory.getLogger(GioHangController.class);

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DonViTinhRepository donViTinhRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam("productId") int productId,
            @RequestParam("donViTinhId") int donViTinhId,
            @RequestParam("quantity") int quantity,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(oAuth2User);

        boolean success = gioHangService.addToCart(productId, donViTinhId, quantity, user, session);
        if (success) {
            response.put("success", true);
            response.put("message", "Sản phẩm đã được thêm vào giỏ hàng!");
            Cart cart = gioHangService.getOrCreateCart(user, session);
            response.put("cartItemCount", gioHangService.getCartItemCount(cart));
        } else {
            response.put("success", false);
            response.put("message", "Không thể thêm sản phẩm vào giỏ hàng!");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items")
    @ResponseBody
    public ResponseEntity<List<CartItemDTO>> getCartItems(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        User user = getAuthenticatedUser(oAuth2User);
        List<CartItemDTO> cartItems = gioHangService.getCartItems(user, session);
        return ResponseEntity.ok(cartItems);
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCartCount(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        User user = getAuthenticatedUser(oAuth2User);
        Cart cart = gioHangService.getOrCreateCart(user, session);
        Map<String, Object> response = new HashMap<>();
        response.put("cartItemCount", gioHangService.getCartItemCount(cart));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @RequestParam("cartItemId") int cartItemId,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(oAuth2User);
        try {
            boolean success = gioHangService.removeFromCart(cartItemId, user, session);
            if (success) {
                response.put("success", true);
                response.put("message", "Sản phẩm đã được xóa khỏi giỏ hàng!");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy mục trong giỏ hàng hoặc không thể xóa!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xóa sản phẩm khỏi giỏ hàng: cartItemId={}", cartItemId, e);
            response.put("success", false);
            response.put("message", "Lỗi server khi xóa sản phẩm: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    private User getAuthenticatedUser(OAuth2User oAuth2User) {
        User user = null;
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            user = userOpt.orElse(null);
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String principal = authentication.getName();
                Optional<User> userOpt = userService.findByPhoneNumber(principal);
                if (userOpt.isEmpty()) {
                    userOpt = userService.findByEmail(principal);
                }
                user = userOpt.orElse(null);
            }
        }
        return user;
    }

    // Proxy endpoints for provinces, districts, wards (giữ nguyên từ mã của bạn)
    @GetMapping("/proxy/provinces")
    @ResponseBody
    public ResponseEntity<?> getProvinces() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://provinces.open-api.vn/api/p/";
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching provinces: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách tỉnh/thành phố");
        }
    }

    @GetMapping("/proxy/districts/{provinceCode}")
    @ResponseBody
    public ResponseEntity<?> getDistricts(@PathVariable String provinceCode) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://provinces.open-api.vn/api/p/" + provinceCode + "?depth=2";
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching districts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách quận/huyện");
        }
    }

    @GetMapping("/proxy/wards/{districtCode}")
    @ResponseBody
    public ResponseEntity<?> getWards(@PathVariable String districtCode) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://provinces.open-api.vn/api/d/" + districtCode + "?depth=2";
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching wards: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách xã/phường");
        }
    }
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @RequestParam("cartItemId") int cartItemId,
            @RequestParam("quantity") int quantity,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(oAuth2User);
        try {
            boolean success = gioHangService.updateCartItemQuantity(cartItemId, quantity, user, session);
            if (success) {
                response.put("success", true);
                response.put("message", "Cập nhật số lượng thành công!");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy mục trong giỏ hàng hoặc bạn không có quyền cập nhật!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật số lượng giỏ hàng: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi server khi cập nhật số lượng!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Trong GioHangController.java
    @GetMapping("/total-quantity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTotalCartItemQuantity(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        User user = getAuthenticatedUser(oAuth2User);
        Cart cart = gioHangService.getOrCreateCart(user, session);
        Map<String, Object> response = new HashMap<>();
        response.put("totalCartItemQuantity", gioHangService.getTotalCartItemQuantity(cart));
        return ResponseEntity.ok(response);
    }



    @GetMapping
    public String viewCart(
            @AuthenticationPrincipal Object principal,
            HttpSession session, Model model) {
        User loggedInUser = null;

        // Kiểm tra đăng nhập qua OAuth2 hoặc UserDetails
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) principal;
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByPhoneNumber(userDetails.getUsername());
            }
            loggedInUser = userOpt.orElse(null);
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userRepository.findByGoogleId(providerId) : userRepository.findByFacebookId(providerId);
            loggedInUser = userOpt.orElse(null);
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String principalName = authentication.getName();
                Optional<User> userOpt = userRepository.findByPhoneNumber(principalName);
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByEmail(principalName);
                }
                loggedInUser = userOpt.orElse(null);
            }
        }

        try {
            Cart cart = gioHangService.getOrCreateCart(loggedInUser, session);
            List<CartItem> cartItems = gioHangService.getCartItemEntities(loggedInUser, session);
            // Tính giá từng sản phẩm, tổng tiền gốc và tổng tiền sau giảm giá
            BigDecimal originalCartTotal = BigDecimal.ZERO;
            BigDecimal directDiscount = BigDecimal.ZERO;
            BigDecimal voucherDiscount = BigDecimal.ZERO; // Giả sử chưa áp dụng voucher

            for (CartItem item : cartItems) {
                BigDecimal unitPrice = item.getDonViTinh().getGia();
                BigDecimal discountedPrice = item.getPrice();
                BigDecimal quantity = new BigDecimal(item.getQuantity());

                BigDecimal itemOriginalTotalPrice = unitPrice.multiply(quantity);
                BigDecimal itemTotalPrice = discountedPrice.multiply(quantity);
                item.setItemTotalPrice(itemTotalPrice);

                originalCartTotal = originalCartTotal.add(itemOriginalTotalPrice);
                if (item.getDonViTinh().hasDiscount()) {
                    BigDecimal discountAmount = (unitPrice.subtract(discountedPrice)).multiply(quantity);
                    directDiscount = directDiscount.add(discountAmount);
                }
            }

            BigDecimal totalSavings = directDiscount.add(voucherDiscount);
            BigDecimal finalTotal = originalCartTotal.subtract(totalSavings);

            // Thêm thông tin vào model
            model.addAttribute("loggedInUser", loggedInUser);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
            model.addAttribute("originalCartTotal", originalCartTotal);
            model.addAttribute("directDiscount", directDiscount);
            model.addAttribute("voucherDiscount", voucherDiscount);
            model.addAttribute("totalSavings", totalSavings);
            model.addAttribute("finalTotal", finalTotal);
        } catch (Exception e) {
            logger.error("Error loading cart: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải giỏ hàng. Vui lòng thử lại sau.");
            model.addAttribute("cartItems", Collections.emptyList());
            model.addAttribute("cartItemCount", 0);
            model.addAttribute("originalCartTotal", BigDecimal.ZERO);
            model.addAttribute("directDiscount", BigDecimal.ZERO);
            model.addAttribute("voucherDiscount", BigDecimal.ZERO);
            model.addAttribute("totalSavings", BigDecimal.ZERO);
            model.addAttribute("finalTotal", BigDecimal.ZERO);
        }

        return "giohang";
    }

    @GetMapping("/default-address")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDefaultAddress(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = getAuthenticatedUser(oAuth2User);

        if (user != null) {
            UserAddress defaultAddress = gioHangService.getDefaultAddress(user);
            if (defaultAddress != null) {
                logger.info("Default address found: city={}, district={}, ward={}",
                        defaultAddress.getCity(), defaultAddress.getDistrict(), defaultAddress.getWard());
                response.put("success", true);
                response.put("fullName", defaultAddress.getFullName());
                response.put("phoneNumber", defaultAddress.getPhoneNumber());
                response.put("city", defaultAddress.getCity());
                response.put("district", defaultAddress.getDistrict());
                response.put("ward", defaultAddress.getWard());
                response.put("addressDetail", defaultAddress.getAddressDetail());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("No default address found for user: {}", user.getId());
                response.put("success", false);
                response.put("message", "Không tìm thấy địa chỉ mặc định!");
                return ResponseEntity.ok(response);
            }
        } else {
            logger.warn("User not authenticated");
            response.put("success", false);
            response.put("message", "Người dùng chưa đăng nhập!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }


}