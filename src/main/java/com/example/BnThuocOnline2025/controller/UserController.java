package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.dto.OrderDTO;
import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.model.UserAddress;
import com.example.BnThuocOnline2025.repository.UserAddressRepository;
import com.example.BnThuocOnline2025.securityconfig.JwtUtil;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.RecaptchaService;
import com.example.BnThuocOnline2025.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RecaptchaService recaptchaService;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private UserAddressRepository userAddressRepository;

    public UserController(UserService userService, JwtUtil jwtUtil, RecaptchaService recaptchaService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.recaptchaService = recaptchaService;
    }

    @ModelAttribute
    public void addUserToModel(Model model, @AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = null;
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String phoneNumber = authentication.getName();
                Optional<User> userOptional = userService.findByPhoneNumber(phoneNumber);
                if (userOptional.isPresent()) {
                    user = userOptional.get();
                }
            }
        }

        if (user != null) {
            model.addAttribute("loggedInUser", user);
            List<UserAddress> addresses = userService.getAddressesByUserId(user.getId());
            model.addAttribute("addresses", addresses);
            List<OrderDTO> orders = userService.getOrdersByUserId(user.getId());
            model.addAttribute("orders", orders);
        }
    }

    @GetMapping("/login")
    public String showLoginPage(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User != null) {
            return "redirect:/";
        }
        return "/";
    }

    @GetMapping("/register")
    public String showRegisterPage(@RequestParam(required = false) String providerId,
                                   @RequestParam(required = false) String provider,
                                   @AuthenticationPrincipal OAuth2User oAuth2User,
                                   Model model) {
        return "redirect:/";
    }

    @PostMapping("/save-profile")
    public String saveProfile(@RequestParam String providerId,
                              @RequestParam String provider,
                              @RequestParam String name,
                              @RequestParam LocalDate dateOfBirth,
                              @RequestParam String address,
                              @RequestParam String phoneNumber,
                              @RequestParam String password) {
        Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setName(name);
            user.setDateOfBirth(dateOfBirth);
            user.setAddress(address);
            user.setPhoneNumber(phoneNumber);
            user.setPassword(passwordEncoder.encode(password));
            userService.saveUser(user);
            return "redirect:/";
        }
        return "redirect:/";
    }

    @GetMapping("/home")
    public String showHomePage(Model model,
                               @RequestParam String providerId,
                               @RequestParam String provider,
                               HttpSession session) {
        Optional<User> userOptional;
        if ("phone".equals(provider)) {
            userOptional = userService.findByPhoneNumber(providerId);
        } else {
            userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
        }

        if (userOptional.isPresent()) {
            User currentUser = userOptional.get();
            model.addAttribute("loggedInUser", currentUser);
            model.addAttribute("name", currentUser.getName());
            model.addAttribute("email", currentUser.getEmail());
            model.addAttribute("phoneNumber", currentUser.getPhoneNumber());
            model.addAttribute("picture", currentUser.getPicture() != null ? currentUser.getPicture() : "/image/profile.png");
            model.addAttribute("gender", currentUser.getGender());
            model.addAttribute("dateOfBirth", currentUser.getDateOfBirth());
            model.addAttribute("address", currentUser.getAddress());
            List<UserAddress> addresses = userService.getAddressesByUserId(currentUser.getId());
            model.addAttribute("addresses", addresses);
            List<OrderDTO> orders = userService.getOrdersByUserId(currentUser.getId());
            model.addAttribute("orders", orders);

//            Cart cart = gioHangService.getOrCreateCart(currentUser, session);
//            model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
//            model.addAttribute("cartItems", gioHangService.getCartItems(currentUser, session));

            return "thongtincanhan";
        }
        return "redirect:/";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest, HttpSession session, HttpServletResponse response) {
        String phoneNumber = loginRequest.get("phoneNumber");
        String password = loginRequest.get("password");
        String recaptchaResponse = loginRequest.get("recaptchaResponse");

        Map<String, Object> responseMap = new HashMap<>();

        if (!recaptchaService.verifyRecaptcha(recaptchaResponse)) {
            responseMap.put("error", "Xác minh reCAPTCHA không thành công!");
            return ResponseEntity.status(400).body(responseMap);
        }

        if (!phoneNumber.matches("[0-9]{10}")) {
            responseMap.put("error", "Số điện thoại phải có 10 chữ số!");
            return ResponseEntity.status(400).body(responseMap);
        }

        try {
            User user = userService.authenticateUser(phoneNumber, password);
            String token = jwtUtil.generateToken(user);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    user.getPhoneNumber(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
            authToken.setDetails(user);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            Cookie tokenCookie = new Cookie("JWT_TOKEN", token);
            tokenCookie.setHttpOnly(true);
            tokenCookie.setPath("/");
            tokenCookie.setMaxAge(10 * 60 * 60);
            response.addCookie(tokenCookie);

            session.setAttribute("token", token);

//            Cart cart = gioHangService.getOrCreateCart(user, session);
//            int cartItemCount = gioHangService.getCartItemCount(cart);

            responseMap.put("token", token);
            responseMap.put("role", user.getRole());
            responseMap.put("phoneNumber", user.getPhoneNumber());
            responseMap.put("name", user.getName() != null ? user.getName() : "Người dùng");
            responseMap.put("picture", user.getPicture() != null ? user.getPicture() : "/image/profile.png");
//            responseMap.put("cartItemCount", cartItemCount);
            responseMap.put("redirectUrl", "/");

            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            responseMap.put("error", e.getMessage());
            return ResponseEntity.status(401).body(responseMap);
        }
    }

    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map<String, String>> register(@RequestParam(required = false) String phoneNumber,
                                                        @RequestParam(required = false) String password,
                                                        @RequestParam(required = false) String recaptchaResponse,
                                                        @RequestBody(required = false) Map<String, String> registerRequest) {
        Map<String, String> response = new HashMap<>();

        if (registerRequest != null) {
            phoneNumber = registerRequest.get("phoneNumber");
            password = registerRequest.get("password");
            recaptchaResponse = registerRequest.get("recaptchaResponse");
        }

        if (phoneNumber == null || password == null || recaptchaResponse == null) {
            response.put("error", "Thiếu thông tin bắt buộc!");
            return ResponseEntity.status(400).body(response);
        }

        if (!recaptchaService.verifyRecaptcha(recaptchaResponse)) {
            response.put("error", "Xác minh reCAPTCHA không thành công!");
            return ResponseEntity.status(400).body(response);
        }

        if (!phoneNumber.matches("[0-9]{10}")) {
            response.put("error", "Số điện thoại phải có 10 chữ số!");
            return ResponseEntity.status(400).body(response);
        }

        if (!isStrongPassword(password)) {
            response.put("error", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return ResponseEntity.status(400).body(response);
        }

        try {
            User user = userService.registerUser(phoneNumber, password);
            response.put("message", "Đăng ký thành công! Vui lòng đăng nhập.");
            response.put("redirectUrl", "/");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    @PostMapping("/updatePersonalInfo")
    @ResponseBody
    public Map<String, Object> updatePersonalInfo(
            @RequestParam String fullName,
            @RequestParam String gender,
            @RequestParam String birthDate,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        User user = null;

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để cập nhật thông tin!");
            return response;
        }

        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        } else {
            String principal = authentication.getName();
            Optional<User> userOptional = userService.findByPhoneNumber(principal);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        }

        if (user == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng!");
            return response;
        }

        user.setName(fullName);
        user.setGender(gender);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            user.setDateOfBirth(LocalDate.parse(birthDate, formatter));
        } catch (DateTimeParseException e) {
            response.put("success", false);
            response.put("message", "Ngày sinh không hợp lệ, vui lòng nhập theo định dạng dd/MM/yyyy");
            return response;
        }

        userService.saveUser(user);

        response.put("success", true);
        response.put("message", "Cập nhật thông tin thành công!");
        response.put("user", Map.of(
                "name", user.getName(),
                "gender", user.getGender(),
                "dateOfBirth", user.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "Chưa cập nhật",
                "email", user.getEmail() != null ? user.getEmail() : "Chưa cập nhật",
                "picture", user.getPicture() != null ? user.getPicture() : ""
        ));

        return response;
    }

    @PostMapping("/addAddress")
    @ResponseBody
    public Map<String, Object> addAddress(
            @ModelAttribute UserAddress address,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        User user = null;

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thêm địa chỉ!");
            return response;
        }

        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        } else {
            String principal = authentication.getName();
            Optional<User> userOptional = userService.findByPhoneNumber(principal);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        }

        if (user == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng!");
            return response;
        }

        if (address.getFullName() == null || address.getFullName().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Họ và tên không được để trống!");
            return response;
        }
        if (address.getPhoneNumber() == null || !address.getPhoneNumber().matches("[0-9]{10}")) {
            response.put("success", false);
            response.put("message", "Số điện thoại phải có 10 chữ số!");
            return response;
        }
        if (address.getCity() == null || address.getDistrict() == null || address.getWard() == null || address.getAddressDetail() == null) {
            response.put("success", false);
            response.put("message", "Vui lòng điền đầy đủ thông tin địa chỉ!");
            return response;
        }
        if (address.getAddressType() == null || !List.of("Nhà", "Văn phòng").contains(address.getAddressType())) {
            response.put("success", false);
            response.put("message", "Loại địa chỉ không hợp lệ!");
            return response;
        }

        address.setUser(user);
        address.setIsDefault(address.getIsDefault() != null && address.getIsDefault());

        if (address.getIsDefault()) {
            List<UserAddress> existingAddresses = userService.getAddressesByUserId(user.getId());
            existingAddresses.forEach(addr -> {
                if (!addr.getId().equals(address.getId())) {
                    addr.setIsDefault(false);
                    userService.saveAddress(addr);
                }
            });
        }

        userService.saveAddress(address);

        List<UserAddress> updatedAddresses = userService.getAddressesByUserId(user.getId());
        response.put("success", true);
        response.put("message", "Thêm địa chỉ thành công!");
        response.put("addresses", updatedAddresses.stream().map(addr -> Map.of(
                "id", addr.getId(),
                "fullName", addr.getFullName(),
                "phoneNumber", addr.getPhoneNumber(),
                "addressDetail", addr.getAddressDetail() + ", " + addr.getWard() + ", " + addr.getDistrict() + ", " + addr.getCity(),
                "addressType", addr.getAddressType(),
                "isDefault", addr.getIsDefault()
        )).toList());

        return response;
    }

    @DeleteMapping("/deleteAddress/{id}")
    @ResponseBody
    public Map<String, Object> deleteAddress(
            @PathVariable("id") Long addressId,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        User user = null;

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để xóa địa chỉ!");
            return response;
        }

        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        } else {
            String principal = authentication.getName();
            Optional<User> userOptional = userService.findByPhoneNumber(principal);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        }

        if (user == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng!");
            return response;
        }

        Optional<UserAddress> addressOptional = userAddressRepository.findById(addressId);
        if (addressOptional.isPresent() && addressOptional.get().getUser().getId().equals(user.getId())) {
            userService.deleteAddress(addressId);
            List<UserAddress> updatedAddresses = userService.getAddressesByUserId(user.getId());
            response.put("success", true);
            response.put("message", "Xóa địa chỉ thành công!");
            response.put("addresses", updatedAddresses.stream().map(addr -> Map.of(
                    "id", addr.getId(),
                    "fullName", addr.getFullName(),
                    "phoneNumber", addr.getPhoneNumber(),
                    "addressDetail", addr.getAddressDetail() + ", " + addr.getWard() + ", " + addr.getDistrict() + ", " + addr.getCity(),
                    "addressType", addr.getAddressType(),
                    "isDefault", addr.getIsDefault()
            )).toList());
        } else {
            response.put("success", false);
            response.put("message", "Địa chỉ không tồn tại hoặc bạn không có quyền xóa!");
        }

        return response;
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        return pattern.matcher(password).matches();
    }
}