package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.dto.ProductDTO;
import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.repository.DanhMucRepository;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.ProductService;
import com.example.BnThuocOnline2025.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private DanhMucRepository danhMucRepository;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal OAuth2User oAuth2User,
                       HttpSession session) {
        User loggedInUser = null;

        // Kiểm tra đăng nhập qua OAuth2 (Google/Facebook)
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOpt.isPresent()) {
                loggedInUser = userOpt.get();
            }
        } else {
            // Kiểm tra đăng nhập bằng số điện thoại/mật khẩu
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String phoneNumber = authentication.getName(); // Số điện thoại từ SecurityContextHolder
                Optional<User> userOpt = userService.findByPhoneNumber(phoneNumber);
                if (userOpt.isPresent()) {
                    loggedInUser = userOpt.get();
                }
            }
        }

        // Nếu người dùng đã đăng nhập
        if (loggedInUser != null) {
            model.addAttribute("loggedInUser", loggedInUser);
            if ("ADMIN".equals(loggedInUser.getRole())) {
                return "quanly"; // ADMIN vào trang quanly.html
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

                Page<Product> productPage = productService.getProducts(page);
                List<DoiTuong> doiTuongList = productService.getAllDoiTuong();

                model.addAttribute("products", productPage.getContent());
                model.addAttribute("totalPages", productPage.getTotalPages());
                model.addAttribute("currentPage", page);
                model.addAttribute("doiTuongList", doiTuongList);

                Cart cart = gioHangService.getOrCreateCart(user, session);
                model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
                model.addAttribute("cartItems", gioHangService.getCartItems(user, session));

                return "quanly";
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

        // Kiểm tra đăng nhập qua OAuth2
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOpt.isPresent()) {
                loggedInUser = userOpt.get();
            }
        } else {
            // Kiểm tra đăng nhập bằng số điện thoại/mật khẩu
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String principal = authentication.getName();
                Optional<User> userOpt = userService.findByPhoneNumber(principal);
                if (userOpt.isEmpty()) {
                    userOpt = userService.findByEmail(principal);
                }
                if (userOpt.isPresent()) {
                    loggedInUser = userOpt.get();
                }
            }
        }

        // Thêm thông tin người dùng vào model
        if (loggedInUser != null) {
            model.addAttribute("loggedInUser", loggedInUser);
            List<UserAddress> addresses = userService.getAddressesByUserId(loggedInUser.getId());
            model.addAttribute("addresses", addresses);
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
            @AuthenticationPrincipal Object principal) {
        Map<String, Object> response = new HashMap<>();
        User loggedInUser = null;

        // Kiểm tra trạng thái đăng nhập
        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOpt = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            loggedInUser = userOpt.orElse(null);
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String principalName = authentication.getName();
                Optional<User> userOpt = userService.findByPhoneNumber(principalName);
                if (userOpt.isEmpty()) {
                    userOpt = userService.findByEmail(principalName);
                }
                loggedInUser = userOpt.orElse(null);
            }
        }

        // Kiểm tra người dùng đã đăng nhập chưa
        if (loggedInUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để gửi đánh giá.");
            return response;
        }

        Product product = productService.getProductById(productId);
        if (product == null) {
            response.put("success", false);
            response.put("message", "Sản phẩm không tồn tại.");
            return response;
        }

        // Sử dụng biến final để lưu loggedInUser
        final User finalLoggedInUser = loggedInUser;
        List<ProductReview> existingReviews = productService.getProductReviews(productId);
        if (existingReviews.stream().anyMatch(review -> review.getUser().getId().equals(finalLoggedInUser.getId()))) {
            response.put("success", false);
            response.put("message", "Bạn đã đánh giá sản phẩm này rồi.");
            return response;
        }

        // Kiểm tra số sao
        if (rating < 1 || rating > 5) {
            response.put("success", false);
            response.put("message", "Số sao phải từ 1 đến 5.");
            return response;
        }

        // Kiểm tra độ dài nội dung đánh giá
        if (reviewContent.length() > 500) {
            response.put("success", false);
            response.put("message", "Nội dung đánh giá không được vượt quá 500 ký tự.");
            return response;
        }

        // Lưu đánh giá
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(loggedInUser);
        review.setRating(rating);
        review.setReviewContent(reviewContent.isEmpty() ? null : reviewContent);

        productService.saveProductReview(review);

        response.put("success", true);
        response.put("message", "Cảm ơn đánh giá của bạn!"); // Cập nhật thông báo (tùy chọn)
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
    public Map<String, Object> checkAuth(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal());
        response.put("authenticated", isAuthenticated);

        if (isAuthenticated) {
            String principal = authentication.getName(); // phoneNumber, email, hoặc providerId
            Optional<User> userOpt = userService.findByPhoneNumber(principal);
            if (userOpt.isEmpty()) {
                userOpt = userService.findByEmail(principal);
            }
            if (userOpt.isEmpty()) {
                userOpt = userService.findByGoogleId(principal);
            }
            if (userOpt.isEmpty()) {
                userOpt = userService.findByFacebookId(principal);
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("user", Map.of(
                        "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                        "name", user.getName() != null ? user.getName() : "Người dùng",
                        "picture", user.getPicture() != null ? user.getPicture() : "/image/profile.png",
                        "role", user.getRole(),
                        "googleId", user.getGoogleId() != null ? user.getGoogleId() : "",
                        "facebookId", user.getFacebookId() != null ? user.getFacebookId() : ""
                ));
            }
        }
        return response;
    }


    @GetMapping("/api/user")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserInfo(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String phoneNumber = authentication.getName();
            Optional<User> userOpt = userService.findByPhoneNumber(phoneNumber);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("phoneNumber", user.getPhoneNumber());
                response.put("name", user.getName());
                response.put("picture", user.getPicture() != null ? user.getPicture() : "");
                response.put("role", user.getRole());
                return ResponseEntity.ok(response);
            }
        }

        response.put("error", "Người dùng chưa đăng nhập!");
        return ResponseEntity.status(401).body(response);
    }

    @GetMapping("/api/danhmuc")
    public ResponseEntity<List<DanhMuc>> getAllDanhMuc() {
        List<DanhMuc> danhMucList = danhMucRepository.findAll();
        return ResponseEntity.ok(danhMucList);
    }
    @GetMapping("/products-by-danhmuc")
    public String getProductsByDanhMuc(@RequestParam("danhMucId") int danhMucId, Model model) {
        List<Product> products = productService.getProductsByDanhMucId(danhMucId);
        model.addAttribute("products", products);
        return "trangchu :: product-by-danhmuc-fragment"; // Trả về fragment mới
    }

    @GetMapping("/api/products/search")
    @ResponseBody
    public List<ProductDTO> searchProducts(@RequestParam("query") String query) {
        List<Product> products = productService.searchProductsByName(query);
        return products.stream().map(product -> {
            ProductDTO dto = new ProductDTO();
            dto.setId(product.getId());
            dto.setTenSanPham(product.getTenSanPham());
            dto.setMainImageUrl(product.getMainImageUrl());
            dto.setLowestPrice(product.getDiscountedPrice());
            return dto;
        }).collect(Collectors.toList());
    }
}