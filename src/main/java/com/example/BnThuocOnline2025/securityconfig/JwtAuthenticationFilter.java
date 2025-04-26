package com.example.BnThuocOnline2025.securityconfig;

import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class JwtAuthenticationFilter implements Filter {
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = null;
        // Kiểm tra token trong header Authorization
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // Kiểm tra token trong cookie
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("JWT_TOKEN".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (token != null) {
            String subject = jwtUtil.extractSubject(token);
            if (subject != null && jwtUtil.validateToken(token, subject)) {
                String role = jwtUtil.extractRole(token);
                Optional<User> userOpt = userService.findByEmail(subject);
                if (userOpt.isEmpty()) {
                    userOpt = userService.findByPhoneNumber(subject);
                }
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getEmail(),
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                    authentication.setDetails(user);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(jakarta.servlet.FilterConfig filterConfig) throws ServletException {}
    @Override
    public void destroy() {}
}