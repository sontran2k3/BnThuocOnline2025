package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.model.UserAddress;
import com.example.BnThuocOnline2025.repository.UserAddressRepository;
import com.example.BnThuocOnline2025.securityconfig.JwtUtil;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.RecaptchaService;
import com.example.BnThuocOnline2025.service.UserService;
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
        // Kiểm tra người dùng đăng nhập qua OAuth2
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> user = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (user.isPresent()) {
                model.addAttribute("loggedInUser", user.get());
                List<UserAddress> addresses = userService.getAddressesByUserId(user.get().getId());
                model.addAttribute("addresses", addresses);
            }
        } else {
            // Kiểm tra người dùng đăng nhập bằng số điện thoại/mật khẩu
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String phoneNumber = authentication.getName(); // Số điện thoại từ SecurityContextHolder
                Optional<User> user = userService.findByPhoneNumber(phoneNumber);
                if (user.isPresent()) {
                    model.addAttribute("loggedInUser", user.get());
                    List<UserAddress> addresses = userService.getAddressesByUserId(user.get().getId());
                    model.addAttribute("addresses", addresses);
                }
            }
        }
    }

    @GetMapping("/login")
    public String showLoginPage(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User != null) {
            return "redirect:/"; // Người dùng đã đăng nhập, chuyển về trang chủ
        }
        return "/"; // Hiển thị trang chủ với modal đăng nhập
    }

    @GetMapping("/register")
    public String showRegisterPage(@RequestParam(required = false) String providerId,
                                   @RequestParam(required = false) String provider,
                                   @AuthenticationPrincipal OAuth2User oAuth2User,
                                   Model model) {
        if (oAuth2User != null) {
            return "redirect:/"; // Người dùng đã đăng nhập, chuyển về trang chủ
        }
        if (providerId != null && provider != null) {
            Optional<User> user = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (user.isPresent()) {
                model.addAttribute("providerId", providerId);
                model.addAttribute("name", user.get().getName());
                model.addAttribute("provider", provider);
                return "dangky";
            }
        }
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
        Optional<User> user = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
        if (user.isPresent()) {
            User currentUser = user.get();
            model.addAttribute("loggedInUser", currentUser);
            model.addAttribute("name", currentUser.getName());
            model.addAttribute("email", currentUser.getEmail());
            model.addAttribute("phoneNumber", currentUser.getPhoneNumber());
            model.addAttribute("picture", currentUser.getPicture());
            model.addAttribute("gender", currentUser.getGender());
            model.addAttribute("dateOfBirth", currentUser.getDateOfBirth());
            model.addAttribute("address", currentUser.getAddress());
            List<UserAddress> addresses = userService.getAddressesByUserId(currentUser.getId());
            model.addAttribute("addresses", addresses);

            // Thêm dữ liệu giỏ hàng
            Cart cart = gioHangService.getOrCreateCart(currentUser, session);
            model.addAttribute("cartItemCount", gioHangService.getCartItemCount(cart));
            model.addAttribute("cartItems", gioHangService.getCartItems(currentUser, session));

            return "thongtincanhan";
        }
        return "redirect:/";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest, HttpSession session) {
        String phoneNumber = loginRequest.get("phoneNumber");
        String password = loginRequest.get("password");
        String recaptchaResponse = loginRequest.get("recaptchaResponse");

        Map<String, Object> response = new HashMap<>();

        // Xác minh reCAPTCHA
        if (!recaptchaService.verifyRecaptcha(recaptchaResponse)) {
            response.put("error", "Xác minh reCAPTCHA không thành công!");
            return ResponseEntity.status(400).body(response);
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("[0-9]{10}")) {
            response.put("error", "Số điện thoại phải có 10 chữ số!");
            return ResponseEntity.status(400).body(response);
        }

        try {
            User user = userService.authenticateUser(phoneNumber, password);
            String token = jwtUtil.generateToken(user);

            // Lưu thông tin người dùng vào SecurityContextHolder
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    user.getPhoneNumber(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
            authToken.setDetails(user);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Lưu token vào session (tùy chọn, nếu cần)
            session.setAttribute("token", token);

            // Cập nhật giỏ hàng
            Cart cart = gioHangService.getOrCreateCart(user, session);
            int cartItemCount = gioHangService.getCartItemCount(cart);

            response.put("token", token);
            response.put("role", user.getRole());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("cartItemCount", cartItemCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map<String, String>> register(@RequestParam(required = false) String phoneNumber,
                                                        @RequestParam(required = false) String password,
                                                        @RequestParam(required = false) String recaptchaResponse,
                                                        @RequestBody(required = false) Map<String, String> registerRequest) {
        Map<String, String> response = new HashMap<>();

        // Xử lý dữ liệu từ form hoặc JSON
        if (registerRequest != null) {
            phoneNumber = registerRequest.get("phoneNumber");
            password = registerRequest.get("password");
            recaptchaResponse = registerRequest.get("recaptchaResponse");
        }

        // Kiểm tra dữ liệu đầu vào
        if (phoneNumber == null || password == null || recaptchaResponse == null) {
            response.put("error", "Thiếu thông tin bắt buộc!");
            return ResponseEntity.status(400).body(response);
        }

        // Xác minh reCAPTCHA
        if (!recaptchaService.verifyRecaptcha(recaptchaResponse)) {
            response.put("error", "Xác minh reCAPTCHA không thành công!");
            return ResponseEntity.status(400).body(response);
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("[0-9]{10}")) {
            response.put("error", "Số điện thoại phải có 10 chữ số!");
            return ResponseEntity.status(400).body(response);
        }

        // Kiểm tra độ mạnh của mật khẩu
        if (!isStrongPassword(password)) {
            response.put("error", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return ResponseEntity.status(400).body(response);
        }

        try {
            User user = userService.registerUser(phoneNumber, password);
            response.put("message", "Đăng ký thành công! Vui lòng đăng nhập.");
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
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        Map<String, Object> response = new HashMap<>();
        String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
        String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
        Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
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
        } else {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng!");
        }

        return response;
    }

    @PostMapping("/addAddress")
    @ResponseBody
    public Map<String, Object> addAddress(
            @ModelAttribute UserAddress address,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        Map<String, Object> response = new HashMap<>();
        String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
        String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
        Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            address.setUser(user);
            address.setIsDefault(address.getIsDefault() != null && address.getIsDefault());
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
        } else {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng!");
        }

        return response;
    }

    @DeleteMapping("/deleteAddress/{id}")
    @ResponseBody
    public Map<String, Object> deleteAddress(
            @PathVariable("id") Long addressId,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        Map<String, Object> response = new HashMap<>();
        String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
        String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
        Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
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
        } else {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng!");
        }

        return response;
    }

    // Hàm kiểm tra độ mạnh của mật khẩu
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        return pattern.matcher(password).matches();
    }
}