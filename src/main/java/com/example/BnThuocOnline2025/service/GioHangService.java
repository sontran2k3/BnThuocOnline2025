package com.example.BnThuocOnline2025.service;

import com.example.BnThuocOnline2025.dto.CartItemDTO;
import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        if (user != null) {
            // Xử lý cho người dùng đã đăng nhập
            List<Cart> carts = cartRepository.findByUser(user);
            if (!carts.isEmpty()) {
                if (carts.size() > 1) {
                    // Có nhiều giỏ hàng, hợp nhất hoặc giữ giỏ hàng mới nhất
                    cart = mergeCarts(carts, user, session);
                } else {
                    cart = carts.get(0);
                }
            } else {
                // Không có giỏ hàng, tạo mới
                cart = new Cart();
                cart.setUser(user);
                cart.setSessionId(session.getId());
                cart = cartRepository.save(cart);
            }
        } else {
            // Xử lý cho khách vãng lai
            String sessionId = session.getId();
            Optional<Cart> cartOptional = cartRepository.findBySessionId(sessionId);
            if (cartOptional.isPresent()) {
                cart = cartOptional.get();
            } else {
                cart = new Cart();
                cart.setSessionId(sessionId);
                cart = cartRepository.save(cart);
            }
        }
        return cart;
    }

    // Phương thức hợp nhất các giỏ hàng
    private Cart mergeCarts(List<Cart> carts, User user, HttpSession session) {
        // Lấy giỏ hàng mới nhất (hoặc theo tiêu chí khác, ví dụ: created_at)
        Cart primaryCart = carts.stream()
                .max(Comparator.comparing(Cart::getId))
                .orElseThrow(() -> new IllegalStateException("No carts found"));

        // Hợp nhất các CartItem từ các giỏ hàng khác
        for (Cart otherCart : carts) {
            if (otherCart.getId() != primaryCart.getId()) {
                List<CartItem> items = cartItemRepository.findByCart(otherCart);
                for (CartItem item : items) {
                    // Kiểm tra xem sản phẩm đã tồn tại trong primaryCart chưa
                    Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductAndDonViTinh(
                            primaryCart, item.getProduct(), item.getDonViTinh());
                    if (existingItem.isPresent()) {
                        // Cộng dồn số lượng
                        CartItem existing = existingItem.get();
                        existing.setQuantity(existing.getQuantity() + item.getQuantity());
                        cartItemRepository.save(existing);
                    } else {
                        // Chuyển CartItem sang primaryCart
                        item.setCart(primaryCart);
                        cartItemRepository.save(item);
                    }
                }
                // Xóa giỏ hàng cũ
                cartRepository.delete(otherCart);
            }
        }

        return primaryCart;
    }


    @Transactional
    public boolean addToCart(int productId, int donViTinhId, int quantity, User user, HttpSession session) {
        try {
            Cart cart = getOrCreateCart(user, session);
            Optional<Product> productOptional = productRepository.findById(productId);
            Optional<DonViTinh> donViTinhOptional = donViTinhRepository.findById(donViTinhId);

            if (productOptional.isEmpty() || donViTinhOptional.isEmpty()) {
                logger.error("Sản phẩm hoặc đơn vị tính không tồn tại: productId={}, donViTinhId={}", productId, donViTinhId);
                return false;
            }

            Product product = productOptional.get();
            DonViTinh donViTinh = donViTinhOptional.get();

            Optional<CartItem> existingItemOptional = cartItemRepository.findByCartAndProductAndDonViTinh(cart, product, donViTinh);

            CartItem cartItem;
            if (existingItemOptional.isPresent()) {
                cartItem = existingItemOptional.get();
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
            } else {
                cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setDonViTinh(donViTinh);
                cartItem.setQuantity(quantity);
                BigDecimal discountedPrice = productService.calculateDiscountedPrice(donViTinh.getGia(), donViTinh.getDiscount());
                cartItem.setPrice(discountedPrice);
            }

            cartItemRepository.save(cartItem);
            return true;
        } catch (Exception e) {
            logger.error("Lỗi khi thêm sản phẩm vào giỏ hàng: {}", e.getMessage(), e);
            return false;
        }
    }

    public int getCartItemCount(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        return cartItems.size(); // Đếm số lượng CartItem (mục riêng biệt)
    }

    public List<CartItemDTO> getCartItems(User user, HttpSession session) {
        Cart cart = getOrCreateCart(user, session);
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        return cartItems.stream().map(CartItemDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public boolean removeFromCart(int cartItemId, User user, HttpSession session) {
        try {
            Cart cart = getOrCreateCart(user, session);
            Optional<CartItem> cartItemOptional = cartItemRepository.findById(cartItemId);
            if (cartItemOptional.isPresent() && cartItemOptional.get().getCart().getId() == cart.getId()) { // Sử dụng == để so sánh int
                cartItemRepository.delete(cartItemOptional.get());
                return true;
            }
            logger.warn("Không tìm thấy CartItem hoặc không thuộc Cart: cartItemId={}, cartId={}", cartItemId, cart.getId());
            return false;
        } catch (Exception e) {
            logger.error("Lỗi khi xóa sản phẩm khỏi giỏ hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi server khi xóa sản phẩm", e);
        }
    }


    @Transactional
    public boolean updateCartItemQuantity(int cartItemId, int quantity, User user, HttpSession session) {
        try {
            Cart cart = getOrCreateCart(user, session);
            Optional<CartItem> cartItemOptional = cartItemRepository.findById(cartItemId);
            if (cartItemOptional.isPresent() && cartItemOptional.get().getCart().getId() == cart.getId()) { // Sử dụng == thay vì equals()
                CartItem cartItem = cartItemOptional.get();
                if (quantity <= 0) {
                    cartItemRepository.delete(cartItem); // Xóa nếu số lượng <= 0
                } else {
                    cartItem.setQuantity(quantity);
                    BigDecimal discountedPrice = productService.calculateDiscountedPrice(
                            cartItem.getDonViTinh().getGia(), cartItem.getDonViTinh().getDiscount());
                    cartItem.setPrice(discountedPrice);
                    cartItemRepository.save(cartItem);
                }
                return true;
            }
            logger.warn("Không tìm thấy CartItem hoặc không thuộc Cart: cartItemId={}, cartId={}", cartItemId, cart.getId());
            return false;
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật số lượng CartItem: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi server khi cập nhật số lượng", e);
        }
    }

    // Trong GioHangService.java
    public int getTotalCartItemQuantity(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum(); // Tổng số lượng sản phẩm
    }

    public List<CartItem> getCartItemEntities(User user, HttpSession session) {
        Cart cart = getOrCreateCart(user, session);
        return cartItemRepository.findByCart(cart);
    }


}