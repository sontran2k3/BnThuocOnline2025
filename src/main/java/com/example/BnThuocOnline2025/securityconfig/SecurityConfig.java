package com.example.BnThuocOnline2025.securityconfig;

import com.example.BnThuocOnline2025.model.Cart;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.service.GioHangService;
import com.example.BnThuocOnline2025.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final GioHangService gioHangService;

    @Autowired
    public SecurityConfig(UserService userService, JwtUtil jwtUtil, GioHangService gioHangService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.gioHangService = gioHangService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Tạo session nếu cần
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/dangkyaccount",
                                "/register",
                                "/api/register",
                                "/save-profile",
                                "/update-price",
                                "/api/quanly/**",
                                "/products",
                                "/inventory-by-product/{productId}",
                                "/giohang",
                                "/payment/**",
                                "/image/**",
                                "/css/**",
                                "/js/**",
                                "/logonewT.png",
                                "/sanpham/**",
                                "/product-images/**",
                                "/cart/**",
                                "/products-by-doituong",
                                "/quanly",
                                "/api/login",
                                "/thanhtoan/success",
                                "/thanhtoan/cancel",
                                "/thanhtoan/payos/webhook",
                                "/thanhtoan/payos",
                                "/submit-review",
                                "/get-reviews",
                                "/check-auth",
                                "/logout",
                                "/cart/proxy/**",
                                "/api/danhmuc",
                                "/thanhtoan/**)",
                                "/products-by-danhmuc",
                                "/api/products/search"
                        ).permitAll()
                        .requestMatchers("/thanhtoan/payos", "/api/user").authenticated()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/quanly/**",
                                "/save-profile",
                                "/products",
                                "/api/login",
                                "/api/register",
                                "/thanhtoan/payos",
                                "/thanhtoan/payos/webhook",
                                "/submit-review",
                                "/get-reviews",
                                "/check-auth",
                                "/cart/proxy/**",
                                "/thanhtoan/**)",
                                "/logout"
                        )
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService())
                        )
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                            Map<String, Object> attributes = oAuth2User.getAttributes();
                            String providerId = attributes.get("sub") != null ? attributes.get("sub").toString() : attributes.get("id").toString();
                            String name = (String) attributes.get("name");
                            String email = (String) attributes.get("email");
                            String provider = request.getRequestURI().contains("google") ? "google" : "facebook";

                            String picture = null;
                            Object pictureObj = attributes.get("picture");
                            if (pictureObj instanceof String) {
                                picture = (String) pictureObj;
                            } else if (pictureObj instanceof Map) {
                                Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
                                Map<String, Object> data = (Map<String, Object>) pictureMap.get("data");
                                if (data != null) {
                                    picture = (String) data.get("url");
                                }
                            }

                            User user = userService.saveOrUpdateUser(providerId, name, picture, email, provider);
                            String token = jwtUtil.generateToken(user);
                            response.addHeader("Authorization", "Bearer " + token);

                            // Lưu token vào cookie
                            Cookie tokenCookie = new Cookie("JWT_TOKEN", token);
                            tokenCookie.setHttpOnly(true);
                            tokenCookie.setPath("/");
                            tokenCookie.setMaxAge(10 * 60 * 60);
                            response.addCookie(tokenCookie);

                            if (!userService.isProfileComplete(user)) {
                                response.sendRedirect("/register?providerId=" + providerId + "&provider=" + provider);
                            } else {
                                if ("ADMIN".equals(user.getRole())) {
                                    response.sendRedirect("/quanly");
                                } else {
                                    response.sendRedirect("/");
                                }
                            }
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .logoutUrl("/logout")
                        .deleteCookies("JWT_TOKEN", "JSESSIONID") // Xóa cả cookie JSESSIONID
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Xóa SecurityContext
                            SecurityContextHolder.clearContext();
                            // Xóa session
                            HttpSession session = request.getSession(false);
                            if (session != null) {
                                session.invalidate();
                            }
                            // Xóa cookie JWT_TOKEN thủ công để đảm bảo
                            Cookie cookie = new Cookie("JWT_TOKEN", null);
                            cookie.setPath("/");
                            cookie.setHttpOnly(true);
                            cookie.setMaxAge(0);
                            response.addCookie(cookie);
                            // Chuyển hướng về trang chủ
                            response.sendRedirect("/");
                        })
                        .permitAll()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return userRequest -> {
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            return oAuth2User;
        };
    }
}