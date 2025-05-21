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
    private UserAddressRepository userAddressRepository;

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
                    cart = mergeCarts(carts, user);
                } else {
                    cart = carts.get(0);
                    // Đảm bảo session_id là null
                    if (cart.getSessionId() != null) {
                        cart.setSessionId(null);
                        cart = cartRepository.save(cart);
                    }
                }
            } else {
                // Không có giỏ hàng, tạo mới với user_id và session_id = null
                cart = new Cart();
                cart.setUser(user);
                cart.setSessionId(null); // Đặt session_id là null
                cart = cartRepository.save(cart);
            }

            // Kiểm tra nếu có giỏ hàng của khách vãng lai (session_id), hợp nhất
            String sessionId = session.getId();
            List<Cart> sessionCarts = cartRepository.findAllBySessionId(sessionId);
            if (!sessionCarts.isEmpty()) {
                for (Cart sessionCart : sessionCarts) {
                    if (sessionCart.getUser() == null) {
                        mergeSessionCartToUserCart(sessionCart, cart);
                        cartRepository.delete(sessionCart);
                    }
                }
            }
        } else {
            // Xử lý cho khách vãng lai
            String sessionId = session.getId();
            List<Cart> carts = cartRepository.findAllBySessionId(sessionId);
            if (carts.isEmpty()) {
                // Không có giỏ hàng, tạo mới
                cart = new Cart();
                cart.setSessionId(sessionId);
                cart.setUser(null); // Đảm bảo user_id là null
                cart = cartRepository.save(cart);
            } else if (carts.size() == 1) {
                // Có đúng một giỏ hàng, sử dụng nó
                cart = carts.get(0);
            } else {
                // Có nhiều giỏ hàng, hợp nhất hoặc chọn giỏ hàng mới nhất
                cart = mergeGuestCarts(carts, sessionId);
            }
        }
        return cart;
    }




    // Thêm phương thức để tìm tất cả giỏ hàng theo sessionId
    private List<Cart> findAllBySessionId(String sessionId) {
        return cartRepository.findAll().stream()
                .filter(cart -> sessionId.equals(cart.getSessionId()))
                .collect(Collectors.toList());
    }

    // Phương thức hợp nhất giỏ hàng của khách vãng lai vào giỏ hàng của người dùng
    private void mergeSessionCartToUserCart(Cart sessionCart, Cart userCart) {
        List<CartItem> sessionItems = cartItemRepository.findByCart(sessionCart);
        for (CartItem item : sessionItems) {
            Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductAndDonViTinh(
                    userCart, item.getProduct(), item.getDonViTinh());
            if (existingItem.isPresent()) {
                // Cộng dồn số lượng
                CartItem existing = existingItem.get();
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                cartItemRepository.save(existing);
            } else {
                // Chuyển CartItem sang userCart
                item.setCart(userCart);
                cartItemRepository.save(item);
            }
        }
    }

    // Phương thức hợp nhất các giỏ hàng của khách vãng lai
    private Cart mergeGuestCarts(List<Cart> carts, String sessionId) {
        // Lấy giỏ hàng mới nhất (dựa trên created_at hoặc id)
        Cart primaryCart = carts.stream()
                .max(Comparator.comparing(Cart::getCreatedAt))
                .orElseThrow(() -> new IllegalStateException("No carts found"));

        // Hợp nhất các CartItem từ các giỏ hàng khác
        for (Cart otherCart : carts) {
            if (otherCart.getId() != primaryCart.getId()) {
                List<CartItem> items = cartItemRepository.findByCart(otherCart);
                for (CartItem item : items) {
                    Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductAndDonViTinh(
                            primaryCart, item.getProduct(), item.getDonViTinh());
                    if (existingItem.isPresent()) {
                        CartItem existing = existingItem.get();
                        existing.setQuantity(existing.getQuantity() + item.getQuantity());
                        cartItemRepository.save(existing);
                    } else {
                        item.setCart(primaryCart);
                        cartItemRepository.save(item);
                    }
                }
                cartRepository.delete(otherCart);
            }
        }
        return primaryCart;
    }

    // Phương thức hợp nhất các giỏ hàng của người dùng đăng nhập
    private Cart mergeCarts(List<Cart> carts, User user) {
        Cart primaryCart = carts.stream()
                .max(Comparator.comparing(Cart::getId))
                .orElseThrow(() -> new IllegalStateException("No carts found"));

        for (Cart otherCart : carts) {
            if (otherCart.getId() != primaryCart.getId()) {
                List<CartItem> items = cartItemRepository.findByCart(otherCart);
                for (CartItem item : items) {
                    Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductAndDonViTinh(
                            primaryCart, item.getProduct(), item.getDonViTinh());
                    if (existingItem.isPresent()) {
                        CartItem existing = existingItem.get();
                        existing.setQuantity(existing.getQuantity() + item.getQuantity());
                        cartItemRepository.save(existing);
                    } else {
                        item.setCart(primaryCart);
                        cartItemRepository.save(item);
                    }
                }
                cartRepository.delete(otherCart);
            }
        }
        // Đảm bảo session_id của primaryCart là null
        primaryCart.setSessionId(null);
        return cartRepository.save(primaryCart);
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

    // Các phương thức khác giữ nguyên
    public int getCartItemCount(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        return cartItems.size();
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
            if (cartItemOptional.isPresent() && cartItemOptional.get().getCart().getId() == cart.getId()) {
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
            if (cartItemOptional.isPresent() && cartItemOptional.get().getCart().getId() == cart.getId()) {
                CartItem cartItem = cartItemOptional.get();
                if (quantity <= 0) {
                    cartItemRepository.delete(cartItem);
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

    public int getTotalCartItemQuantity(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public List<CartItem> getCartItemEntities(User user, HttpSession session) {
        Cart cart = getOrCreateCart(user, session);
        return cartItemRepository.findByCart(cart);
    }



    @Transactional
    public UserAddress getDefaultAddress(User user) {
        if (user == null) {
            return null;
        }
        return userAddressRepository.findByUserAndIsDefault(user, true).orElse(null);
    }
}