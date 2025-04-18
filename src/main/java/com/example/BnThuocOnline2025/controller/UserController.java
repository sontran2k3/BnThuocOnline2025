package com.example.BnThuocOnline2025.controller;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.securityconfig.JwtUtil;
import com.example.BnThuocOnline2025.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap; // Thêm import này
import java.util.Map;
import java.util.Optional;
import java.util.UUID;@Controller
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

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
            user.setAddress(address); // Lưu chuỗi địa chỉ từ dropdown
            user.setPhoneNumber(phoneNumber);
            user.setPassword(password);
            userService.saveUser(user);
            return "redirect:/";
        }
        return "redirect:/";
    }

    @GetMapping("/home")
    public String showHomePage(Model model, @RequestParam String providerId, @RequestParam String provider) {
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
    public String updatePersonalInfo(
            @RequestParam String fullName,
            @RequestParam String gender,
            @RequestParam String birthDate,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            Model model) {
        // Lấy thông tin người dùng hiện tại
        String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
        String provider = oAuth2User.getAttribute("sub") != null ? "google" : "facebook";
        Optional<User> userOptional = "google".equals(provider) ? userService.findByGoogleId(providerId) : userService.findByFacebookId(providerId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Cập nhật thông tin
            user.setName(fullName);
            user.setGender(gender);
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                user.setDateOfBirth(LocalDate.parse(birthDate, formatter));
            } catch (DateTimeParseException e) {
                model.addAttribute("error", "Ngày sinh không hợp lệ, vui lòng nhập theo định dạng dd/MM/yyyy");
                // Trả lại dữ liệu hiện tại để hiển thị
                model.addAttribute("loggedInUser", user);
                model.addAttribute("name", user.getName());
                model.addAttribute("phoneNumber", user.getPhoneNumber());
                model.addAttribute("gender", user.getGender());
                model.addAttribute("dateOfBirth", user.getDateOfBirth());
                model.addAttribute("picture", user.getPicture());
                model.addAttribute("email", user.getEmail());
                return "thongtincanhan";
            }

            // Lưu thông tin
            userService.saveUser(user);

            // Thêm thông báo thành công
            model.addAttribute("success", "Cập nhật thông tin thành công!");
            // Cập nhật model
            model.addAttribute("loggedInUser", user);
            model.addAttribute("name", user.getName());
            model.addAttribute("phoneNumber", user.getPhoneNumber());
            model.addAttribute("gender", user.getGender());
            model.addAttribute("dateOfBirth", user.getDateOfBirth());
            model.addAttribute("picture", user.getPicture());
            model.addAttribute("email", user.getEmail());
        } else {
            model.addAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/";
        }

        return "thongtincanhan";
    }

}

