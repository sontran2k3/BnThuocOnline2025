package com.example.BnThuocOnline2025.service;

import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GioHangService {

    private static final Logger logger = LoggerFactory.getLogger(GioHangService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DonViTinhRepository donViTinhRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductService productService;

    @Transactional
    public Cart getOrCreateCart(User user, HttpSession session) {
        Cart cart;

        if (user != null && user.getId() != null) {
            logger.info("Finding or creating cart for user: {}", user.getId());
            List<Cart> userCarts = cartRepository.findAllByUser(user);
            if (userCarts.isEmpty()) {
                cart = new Cart();
                cart.setUser(user);
                cart.setCartItems(new ArrayList<>());
            } else if (userCarts.size() == 1) {
                cart = userCarts.get(0);
            } else {
                cart = userCarts.get(0);
                for (int i = 1; i < userCarts.size(); i++) {
                    Cart duplicateCart = userCarts.get(i);
                    if (duplicateCart.getCartItems() != null) {
                        for (CartItem item : duplicateCart.getCartItems()) {
                            item.setCart(cart);
                            if (!cart.getCartItems().contains(item)) {
                                cart.getCartItems().add(item);
                            }
                        }
                    }
                    cartRepository.delete(duplicateCart);
                }
            }

            String sessionId = (String) session.getAttribute("cartSessionId");
            if (sessionId != null) {
                Optional<Cart> sessionCart = cartRepository.findBySessionId(sessionId);
                if (sessionCart.isPresent() && sessionCart.get().getUser() == null) {
                    Cart existingCart = sessionCart.get();
                    if (existingCart.getCartItems() != null) {
                        for (CartItem item : existingCart.getCartItems()) {
                            item.setCart(cart);
                            if (!cart.getCartItems().contains(item)) {
                                cart.getCartItems().add(item);
                            }
                        }
                    }
                    existingCart.setUser(user);
                    existingCart.setSessionId(null);
                    cartRepository.save(existingCart);
                    session.removeAttribute("cartSessionId");
                    return existingCart;
                }
            }
        } else {
            String sessionId = (String) session.getAttribute("cartSessionId");
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                session.setAttribute("cartSessionId", sessionId);
            }
            logger.info("Finding or creating cart for sessionId: {}", sessionId);
            String finalSessionId = sessionId;
            cart = cartRepository.findBySessionId(sessionId).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setSessionId(finalSessionId);
                newCart.setCartItems(new ArrayList<>());
                return newCart;
            });
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart addToCart(User user, HttpSession session, int productId, int donViTinhId, int quantity) {
        Cart cart = getOrCreateCart(user, session);

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại với ID: " + productId);
        }
        Product product = productOpt.get();

        Optional<DonViTinh> donViTinhOpt = donViTinhRepository.findById(donViTinhId);
        if (donViTinhOpt.isEmpty()) {
            throw new IllegalArgumentException("Đơn vị tính không tồn tại với ID: " + donViTinhId);
        }
        DonViTinh donViTinh = donViTinhOpt.get();

        if (!product.getDonViTinhList().contains(donViTinh)) {
            throw new IllegalArgumentException("Đơn vị tính ID " + donViTinhId + " không thuộc sản phẩm ID " + productId);
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductAndDonViTinh(cart, product, donViTinh);
        CartItem item;
        if (existingItem.isPresent()) {
            item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity <= 0) {
                cartItemRepository.delete(item); // Xóa nếu số lượng <= 0
                cart.getCartItems().remove(item);
            } else {
                item.setQuantity(newQuantity);
                item.setPrice(donViTinh.getDiscountedPrice());
                cartItemRepository.save(item);
            }
        } else if (quantity > 0) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setDonViTinh(donViTinh);
            item.setQuantity(quantity);
            item.setPrice(donViTinh.getDiscountedPrice());
            if (cart.getCartItems() == null) {
                cart.setCartItems(new ArrayList<>());
            }
            cart.getCartItems().add(item);
            cartItemRepository.save(item);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void removeFromCart(int cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    public int getCartItemCount(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart); // Lấy trực tiếp từ DB để đảm bảo dữ liệu mới nhất
        Map<String, CartItem> uniqueItems = new HashMap<>();
        for (CartItem item : cartItems) {
            if (item.getDonViTinh() == null) {
                logger.warn("CartItem {} (productId={}) has null DonViTinh, skipping count", item.getId(), item.getProduct().getId());
                continue;
            }
            String key = item.getProduct().getId() + "-" + item.getDonViTinh().getId();
            uniqueItems.put(key, item);
        }
        return uniqueItems.size();
    }


    public List<CartItem> getCartItems(User user, HttpSession session) {
        Cart cart = getOrCreateCart(user, session);
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        // Tải chi tiết sản phẩm cho từng CartItem
        for (CartItem item : cartItems) {
            if (item.getProduct() != null) {
                productService.loadProductDetails(item.getProduct());
            }
        }

        // Gộp các CartItem trùng lặp dựa trên productId và donViTinhId
        Map<String, CartItem> mergedItems = new HashMap<>();
        for (CartItem item : cartItems) {
            if (item.getDonViTinh() == null) {
                logger.warn("CartItem {} has null DonViTinh, skipping merge", item.getId());
                continue;
            }
            String key = item.getProduct().getId() + "-" + item.getDonViTinh().getId();
            if (mergedItems.containsKey(key)) {
                CartItem existingItem = mergedItems.get(key);
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                cartItemRepository.delete(item); // Xóa mục trùng lặp trong DB
            } else {
                mergedItems.put(key, item);
            }
        }

        return new ArrayList<>(mergedItems.values());
    }

}