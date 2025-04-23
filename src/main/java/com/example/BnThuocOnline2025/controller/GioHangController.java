package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.dto.CartItemDTO;
import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.CartItem;
import com.example.BnThuocOnline2025.model.DonViTinh;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.repository.DonViTinhRepository;
import com.example.BnThuocOnline2025.repository.UserRepository;
import com.example.BnThuocOnline2025.service.GioHangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

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
    private DonViTinhRepository donViTinhRepository;



    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            HttpSession session,
            @RequestParam("productId") int productId,
            @RequestParam("donViTinhId") int donViTinhId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity) {
        User user = getUserFromDetails(userDetails);
        Map<String, Object> response = new HashMap<>();
        try {
            Cart cart = gioHangService.addToCart(user, session, productId, donViTinhId, quantity);
            int cartItemCount = gioHangService.getCartItemCount(cart);
            response.put("success", true);
            response.put("cartItemCount", cartItemCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/items")
    @ResponseBody
    public ResponseEntity<List<CartItemDTO>> getCartItems(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            HttpSession session) {
        User user = getUserFromDetails(userDetails);
        try {
            List<CartItem> cartItems = gioHangService.getCartItems(user, session);
            List<CartItemDTO> cartItemDTOs = cartItems.stream()
                    .map(CartItemDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(cartItemDTOs);
        } catch (Exception e) {
            logger.error("Error fetching cart items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getCartCount(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            HttpSession session) {
        User user = getUserFromDetails(userDetails);
        try {
            Cart cart = gioHangService.getOrCreateCart(user, session);
            int cartItemCount = gioHangService.getCartItemCount(cart);
            Map<String, Integer> response = new HashMap<>();
            response.put("cartItemCount", cartItemCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching cart count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public Map<String, Object> removeFromCart(@RequestParam("cartItemId") int cartItemId) {
        gioHangService.removeFromCart(cartItemId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return response;
    }

    @PostMapping("/update-quantity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantity(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            HttpSession session,
            @RequestParam("productId") int productId,
            @RequestParam("donViTinhId") int donViTinhId,
            @RequestParam("quantity") int quantity) {
        User user = getUserFromDetails(userDetails);
        Map<String, Object> response = new HashMap<>();
        try {
            Cart cart = gioHangService.addToCart(user, session, productId, donViTinhId, quantity - gioHangService.getCartItems(user, session)
                    .stream()
                    .filter(item -> item.getProduct().getId() == productId && item.getDonViTinh().getId() == donViTinhId)
                    .findFirst().map(CartItem::getQuantity).orElse(0));
            response.put("success", true);
            response.put("price", gioHangService.getCartItems(user, session)
                    .stream()
                    .filter(item -> item.getProduct().getId() == productId && item.getDonViTinh().getId() == donViTinhId)
                    .findFirst().map(CartItem::getPrice).orElse(null));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCart(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            HttpSession session) {
        User user = getUserFromDetails(userDetails);
        Map<String, Object> response = new HashMap<>();
        try {
            Cart cart = gioHangService.getOrCreateCart(user, session);
            gioHangService.getCartItems(user, session).forEach(item -> gioHangService.removeFromCart(item.getId()));
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public String viewCart(
            @AuthenticationPrincipal Object principal,
            HttpSession session, Model model) {
        User user = null;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) principal;
            user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            user = "google".equals(provider) ? userRepository.findByGoogleId(providerId).orElse(null)
                    : userRepository.findByFacebookId(providerId).orElse(null);
        }

        try {
            Cart cart = gioHangService.getOrCreateCart(user, session);
            List<CartItem> cartItems = gioHangService.getCartItems(user, session);

            // Tính giá từng sản phẩm, tổng tiền gốc và tổng tiền sau giảm giá
            BigDecimal originalCartTotal = BigDecimal.ZERO; // Tổng tiền gốc
            BigDecimal cartTotal = BigDecimal.ZERO; // Tổng tiền sau giảm giá
            BigDecimal directDiscount = BigDecimal.ZERO; // Giảm giá trực tiếp
            BigDecimal voucherDiscount = BigDecimal.ZERO; // Giảm giá voucher (hiện tại là 0)

            for (CartItem item : cartItems) {
                BigDecimal unitPrice = item.getDonViTinh().getGia(); // Giá gốc
                BigDecimal discountedPrice = item.getPrice(); // Giá sau giảm giá
                BigDecimal quantity = new BigDecimal(item.getQuantity());

                // Tính tổng giá gốc của sản phẩm
                BigDecimal itemOriginalTotalPrice = unitPrice.multiply(quantity);
                // Tính tổng giá sau giảm giá của sản phẩm
                BigDecimal itemTotalPrice = discountedPrice.multiply(quantity);
                item.setItemTotalPrice(itemTotalPrice);

                // Cộng vào tổng tiền gốc và tổng tiền sau giảm giá
                originalCartTotal = originalCartTotal.add(itemOriginalTotalPrice);
                cartTotal = cartTotal.add(itemTotalPrice);

                // Tính giảm giá trực tiếp cho sản phẩm này
                if (item.getDonViTinh().hasDiscount()) {
                    BigDecimal discountAmount = (unitPrice.subtract(discountedPrice)).multiply(quantity);
                    directDiscount = directDiscount.add(discountAmount);
                }
            }

            // Tính tổng tiết kiệm
            BigDecimal totalSavings = directDiscount.add(voucherDiscount);

            // Tính tổng thanh toán
            BigDecimal finalTotal = cartTotal.subtract(totalSavings);

            model.addAttribute("loggedInUser", user);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
            model.addAttribute("originalCartTotal", originalCartTotal); // Thêm tổng tiền gốc
            model.addAttribute("cartTotal", cartTotal);
            model.addAttribute("directDiscount", directDiscount);
            model.addAttribute("voucherDiscount", voucherDiscount);
            model.addAttribute("totalSavings", totalSavings);
            model.addAttribute("finalTotal", finalTotal);
        } catch (Exception e) {
            logger.error("Error loading cart: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải giỏ hàng. Vui lòng thử lại sau.");
            model.addAttribute("cartItems", Collections.emptyList());
            model.addAttribute("cartItemCount", 0);
            model.addAttribute("originalCartTotal", BigDecimal.ZERO); // Thêm tổng tiền gốc
            model.addAttribute("cartTotal", BigDecimal.ZERO);
            model.addAttribute("directDiscount", BigDecimal.ZERO);
            model.addAttribute("voucherDiscount", BigDecimal.ZERO);
            model.addAttribute("totalSavings", BigDecimal.ZERO);
            model.addAttribute("finalTotal", BigDecimal.ZERO);
        }

        return "giohang";
    }

    private User getUserFromDetails(org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails != null) {
            return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    @GetMapping("/get-unit-price")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnitPrice(
            @RequestParam("productId") int productId,
            @RequestParam("donViTinhId") int donViTinhId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<DonViTinh> donViTinhOpt = donViTinhRepository.findById(donViTinhId);
            if (donViTinhOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Đơn vị tính không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
            DonViTinh donViTinh = donViTinhOpt.get();
            response.put("success", true);
            response.put("originalPrice", donViTinh.getGia());
            response.put("discountedPrice", donViTinh.getDiscountedPrice());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}