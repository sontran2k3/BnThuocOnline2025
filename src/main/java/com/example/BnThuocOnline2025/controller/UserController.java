package com.example.BnThuocOnline2025.controller;

import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.model.UserAddress;
import com.example.BnThuocOnline2025.repository.UserAddressRepository;
import com.example.BnThuocOnline2025.securityconfig.JwtUtil;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private UserAddressRepository userAddressRepository;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @ModelAttribute
    public void addUserToModel(Model model, @AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User != null) {
            String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
            String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
            Optional<User> user = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
            if (user.isPresent()) {
                model.addAttribute("loggedInUser", user.get());
                List<UserAddress> addresses = userService.getAddressesByUserId(user.get().getId());
                model.addAttribute("addresses", addresses);
            }
        }
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "/";
    }

    @GetMapping("/register")
    public String showRegisterPage(@RequestParam String providerId, @RequestParam String provider, Model model) {
        Optional<User> user = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);
        if (user.isPresent()) {
            model.addAttribute("providerId", providerId);
            model.addAttribute("name", user.get().getName());
            model.addAttribute("provider", provider);
            return "dangky";
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
            user.setPassword(password);
            userService.saveUser(user);
            return "redirect:/";
        }
        return "redirect:/";
    }

    @GetMapping("/home")
    public String showHomePage(Model model, @RequestParam String providerId, @RequestParam String provider, HttpSession session) {
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
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String phoneNumber = loginRequest.get("phoneNumber");
        String id = loginRequest.get("id");
        String password = loginRequest.get("password");

        Optional<User> userOpt = Optional.empty();
        Map<String, String> response = new HashMap<>();

        if (email != null) {
            userOpt = userService.findByEmail(email);
        } else if (phoneNumber != null) {
            userOpt = userService.findByPhoneNumber(phoneNumber);
        } else if (id != null) {
            try {
                UUID uuid = UUID.fromString(id);
                userOpt = userService.findById(uuid);
            } catch (IllegalArgumentException e) {
                response.put("error", "Invalid ID format");
                return ResponseEntity.status(400).body(response);
            }
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                String token = jwtUtil.generateToken(user);
                response.put("token", token);
                response.put("role", user.getRole());
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Invalid password");
                return ResponseEntity.status(401).body(response);
            }
        } else {
            response.put("error", "User not found");
            return ResponseEntity.status(404).body(response);
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
                    "email", user.getEmail(),
                    "picture", user.getPicture()
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
                    "id", addr.getId(), // Thêm trường id
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
            // Kiểm tra xem địa chỉ có thuộc về người dùng hiện tại không
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
}