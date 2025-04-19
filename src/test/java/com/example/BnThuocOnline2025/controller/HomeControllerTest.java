//package com.example.BnThuocOnline2025.controller;
//
//import com.example.BnThuocOnline2025.model.*;
//import com.example.BnThuocOnline2025.service.GioHangService;
//import com.example.BnThuocOnline2025.service.ProductService;
//import com.example.BnThuocOnline2025.service.UserService;
//
//import jakarta.servlet.http.HttpSession;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Import;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.*;
//
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(HomeController.class)
//@Import(HomeControllerTest.Config.class)
//class HomeControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ProductService productService;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private GioHangService gioHangService;
//
//    @Configuration
//    static class Config {
//        @Bean
//        public ProductService productService() {
//            return Mockito.mock(ProductService.class);
//        }
//
//        @Bean
//        public UserService userService() {
//            return Mockito.mock(UserService.class);
//        }
//
//        @Bean
//        public GioHangService gioHangService() {
//            return Mockito.mock(GioHangService.class);
//        }
//    }
//
//    @Test
//    void home_shouldReturnTrangChu_whenNotLoggedIn() throws Exception {
//        // Mock dữ liệu sản phẩm
//        List<Product> mockProducts = List.of(new Product());
//        when(productService.getProducts(0)).thenReturn(new PageImpl<>(mockProducts, PageRequest.of(0, 10), 1));
//        when(productService.getAllDoiTuong()).thenReturn(List.of(new DoiTuong()));
//
//        // Mock giỏ hàng
//        Cart cart = new Cart();
//        when(gioHangService.getOrCreateCart(null, null)).thenReturn(cart);
//        when(gioHangService.getCartItemCount(cart)).thenReturn(0);
//        when(gioHangService.getCartItems(null, null)).thenReturn(List.of());
//
//        mockMvc.perform(get("/"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("trangchu"))
//                .andExpect(model().attributeExists("products", "totalPages", "currentPage", "doiTuongList", "cartItemCount", "cartItems"));
//    }
//
//    @Test
//    void home_shouldRedirectToQuanLy_whenAdminLoggedIn() throws Exception {
//        // Giả lập OAuth2User
//        Map<String, Object> attributes = new HashMap<>();
//        attributes.put("sub", "google123");
//        OAuth2User oAuth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
//
//        // Tạo user admin mock
//        User adminUser = new User();
//        adminUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
//        adminUser.setRole("ADMIN");
//
//        when(userService.findByGoogleId("google123")).thenReturn(Optional.of(adminUser));
//
//        mockMvc.perform(get("/").requestAttr("org.springframework.security.oauth2.core.user.DefaultOAuth2User", oAuth2User))
//                .andExpect(status().isOk())
//                .andExpect(view().name("quanly"));
//    }
//}
