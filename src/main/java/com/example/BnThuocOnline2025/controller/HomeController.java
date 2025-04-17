package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.ProductService;
import com.example.BnThuocOnline2025.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private GioHangService gioHangService;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal OAuth2User oAuth2User,
                       HttpSession session) {
        User loggedInUser = null;
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOpt.isPresent()) {
                loggedInUser = userOpt.get();
                model.addAttribute("loggedInUser", loggedInUser);
                if ("ADMIN".equals(loggedInUser.getRole())) {
                    return "quanly"; // ADMIN vào trang quanly.html
                }
            }
        }

        // Logic sản phẩm và đối tượng
        Page<Product> productPage = productService.getProducts(page);
        List<DoiTuong> doiTuongList = productService.getAllDoiTuong();

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("doiTuongList", doiTuongList);

        // Logic giỏ hàng
        Cart cart = gioHangService.getOrCreateCart(loggedInUser, session);
        model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
        model.addAttribute("cartItems", gioHangService.getCartItems(loggedInUser, session));

        return "trangchu"; // USER hoặc không đăng nhập vào trang chủ
    }

    @GetMapping("/quanly")
    public String quanLy(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @AuthenticationPrincipal OAuth2User oAuth2User,
                         HttpSession session) {
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!"ADMIN".equals(user.getRole())) {
                    return "redirect:/"; // Nếu không phải ADMIN, chuyển về trang chủ
                }
                model.addAttribute("loggedInUser", user);

                // Logic sản phẩm và đối tượng
                Page<Product> productPage = productService.getProducts(page);
                List<DoiTuong> doiTuongList = productService.getAllDoiTuong();

                model.addAttribute("products", productPage.getContent());
                model.addAttribute("totalPages", productPage.getTotalPages());
                model.addAttribute("currentPage", page);
                model.addAttribute("doiTuongList", doiTuongList);

                // Logic giỏ hàng (nếu cần cho trang quản lý)
                Cart cart = gioHangService.getOrCreateCart(user, session);
                model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
                model.addAttribute("cartItems", gioHangService.getCartItems(user, session));

                return "quanly"; // Trả về quanly.html
            } else {
                return "redirect:/"; // Nếu không tìm thấy user, chuyển về trang chủ
            }
        } else {
            return "redirect:/"; // Nếu chưa đăng nhập, chuyển về trang chủ
        }
    }

    @GetMapping("/sanpham")
    public String productDetail(@RequestParam("id") int productId, Model model,
                                @AuthenticationPrincipal OAuth2User oAuth2User,
                                HttpSession session) {
        Product product = productService.getProductById(productId);
        List<ProductImage> images = productService.getProductImages(productId);
        ChiTietSanPham chiTietSanPham = productService.getChiTietSanPhamByProductId(productId);
        List<CauHoiLienQuan> cauHoiLienQuan = productService.getCauHoiLienQuanByProductId(productId);
        List<Product> relatedProducts = productService.getRelatedProducts(productId, 6);

        if (product == null) {
            return "redirect:/";
        }

        // Thêm thông tin người dùng vào model
        User loggedInUser = null;
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOpt.isPresent()) {
                loggedInUser = userOpt.get();
                model.addAttribute("loggedInUser", loggedInUser);
            }
        }

        // Thêm thông tin giỏ hàng
        Cart cart = gioHangService.getOrCreateCart(loggedInUser, session);
        model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
        model.addAttribute("cartItems", gioHangService.getCartItems(loggedInUser, session));

        // Thêm thông tin sản phẩm
        model.addAttribute("product", product);
        model.addAttribute("images", images);
        model.addAttribute("chiTietSanPham", chiTietSanPham);
        model.addAttribute("cauHoiLienQuan", cauHoiLienQuan);
        model.addAttribute("products", relatedProducts);

        return "sanpham";
    }

    @GetMapping("/products-by-doituong")
    public String getProductsByDoiTuong(@RequestParam("doiTuongId") int doiTuongId, Model model) {
        List<Product> products = productService.getProductsByDoiTuongId(doiTuongId);
        model.addAttribute("products", products);
        return "trangchu :: product-by-doituong-fragment";
    }

    @GetMapping("/update-price")
    @ResponseBody
    public Map<String, Object> updatePrice(@RequestParam("productId") int productId,
                                           @RequestParam("donViTinhId") int donViTinhId) {
        Map<String, Object> response = new HashMap<>();
        Product product = productService.getProductById(productId);
        DonViTinh selectedDonViTinh = productService.getDonViTinhById(productId, donViTinhId);

        if (selectedDonViTinh != null && product != null) {
            BigDecimal discountedPrice = productService.calculateDiscountedPrice(
                    selectedDonViTinh.getGia(),
                    selectedDonViTinh.getDiscount()
            );
            response.put("discountedPrice", discountedPrice);
            response.put("gia", selectedDonViTinh.getGia());
            response.put("donViTinh", selectedDonViTinh.getDonViTinh());
            response.put("hasDiscount", selectedDonViTinh.hasDiscount());
            response.put("discount", selectedDonViTinh.getDiscount() != null ? selectedDonViTinh.getDiscount() : BigDecimal.ZERO);
            response.put("quyCach", product.getQuyCach());
        } else {
            response.put("error", "Không tìm thấy đơn vị tính hoặc sản phẩm.");
        }
        return response;
    }


    @PostMapping("/submit-review")
    @ResponseBody
    public Map<String, Object> submitReview(
            @RequestParam("productId") int productId,
            @RequestParam("rating") float rating,
            @RequestParam("reviewContent") String reviewContent,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        Map<String, Object> response = new HashMap<>();

        // Kiểm tra đăng nhập
        if (oAuth2User == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để gửi đánh giá.");
            return response;
        }

        // Lấy thông tin người dùng
        String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
        String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
        Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);

        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy thông tin người dùng.");
            return response;
        }

        User user = userOpt.get();
        Product product = productService.getProductById(productId);

        if (product == null) {
            response.put("success", false);
            response.put("message", "Sản phẩm không tồn tại.");
            return response;
        }

        // Kiểm tra đánh giá trùng lặp
        List<ProductReview> existingReviews = productService.getProductReviews(productId);
        if (existingReviews.stream().anyMatch(review -> review.getUser().getId().equals(user.getId()))) {
            response.put("success", false);
            response.put("message", "Bạn đã đánh giá sản phẩm này rồi.");
            return response;
        }

        // Kiểm tra dữ liệu đầu vào
        if (rating < 1 || rating > 5) {
            response.put("success", false);
            response.put("message", "Số sao phải từ 1 đến 5.");
            return response;
        }
        if (reviewContent.length() > 500) {
            response.put("success", false);
            response.put("message", "Nội dung đánh giá không được vượt quá 500 ký tự.");
            return response;
        }

        // Lưu đánh giá
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setReviewContent(reviewContent.isEmpty() ? null : reviewContent);

        productService.saveProductReview(review);

        response.put("success", true);
        response.put("message", "Đánh giá đã được gửi thành công!");
        return response;
    }

    @GetMapping("/get-reviews")
    @ResponseBody
    public Map<String, Object> getReviews(
            @RequestParam("productId") int productId,
            @RequestParam(value = "rating", required = false, defaultValue = "0") float rating) {
        Map<String, Object> response = new HashMap<>();

        // Lấy danh sách đánh giá
        List<ProductReview> reviews = rating == 0
                ? productService.getProductReviews(productId) // Tất cả đánh giá
                : productService.getProductReviewsByRating(productId, rating); // Đánh giá theo số sao

        Double averageRating = productService.getAverageRating(productId);
        long reviewCount = productService.getReviewCount(productId);
        Map<Integer, Long> ratingCounts = productService.getReviewCountByRating(productId);

        response.put("reviews", reviews.stream().map(review -> {
            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("userName", review.getUser().getName());
            reviewData.put("rating", review.getRating());
            reviewData.put("reviewContent", review.getReviewContent());
            reviewData.put("createdAt", review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            return reviewData;
        }).collect(Collectors.toList()));
        response.put("averageRating", averageRating);
        response.put("reviewCount", reviewCount);
        response.put("ratingCounts", ratingCounts);

        return response;
    }



    @GetMapping("/check-auth")
    @ResponseBody
    public Map<String, Boolean> checkAuth(@AuthenticationPrincipal OAuth2User oAuth2User) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("authenticated", oAuth2User != null);
        return response;
    }
}