package com.example.BnThuocOnline2025.service;

import com.example.BnThuocOnline2025.dto.OrderDTO;
import com.example.BnThuocOnline2025.dto.OrderItemDTO;
import com.example.BnThuocOnline2025.model.Orders;
import com.example.BnThuocOnline2025.model.OrderItem;
import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.model.UserAddress;
import com.example.BnThuocOnline2025.repository.OrdersRepository;
import com.example.BnThuocOnline2025.repository.UserAddressRepository;
import com.example.BnThuocOnline2025.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private OrdersRepository orderRepository;

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, UserAddressRepository userAddressRepository) {
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
    }

    public User saveOrUpdateUser(String providerId, String name, String picture, String email, String provider) {
        Optional<User> existingUser;
        if ("google".equals(provider)) {
            existingUser = userRepository.findByGoogleId(providerId);
        } else if ("facebook".equals(provider)) {
            existingUser = userRepository.findByFacebookId(providerId);
        } else {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(name);
            user.setPicture(picture);
            return userRepository.save(user);
        } else {
            User newUser = new User(
                    "facebook".equals(provider) ? providerId : null,
                    "google".equals(provider) ? providerId : null,
                    name,
                    picture,
                    email
            );
            return userRepository.save(newUser);
        }
    }

    public Optional<User> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    public Optional<User> findByFacebookId(String facebookId) {
        return userRepository.findByFacebookId(facebookId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public User saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public boolean isProfileComplete(User user) {
        return user.getDateOfBirth() != null &&
                user.getAddress() != null &&
                user.getPhoneNumber() != null &&
                user.getPassword() != null;
    }

    public UserAddress saveAddress(UserAddress address) {
        if (address.getIsDefault()) {
            List<UserAddress> defaultAddresses = userAddressRepository.findAllByUserAndIsDefault(address.getUser(), true);
            for (UserAddress defaultAddress : defaultAddresses) {
                defaultAddress.setIsDefault(false);
                userAddressRepository.save(defaultAddress);
            }
        }
        return userAddressRepository.save(address);
    }

    public List<UserAddress> getAddressesByUserId(UUID userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(userAddressRepository::findByUser).orElse(List.of());
    }

    public void deleteAddress(Long addressId) {
        userAddressRepository.deleteById(addressId);
    }

    public User registerUser(String phoneNumber, String password) throws Exception {
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new Exception("Số điện thoại đã được đăng ký!");
        }
        User newUser = new User();
        newUser.setPhoneNumber(phoneNumber);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("USER");
        return userRepository.save(newUser);
    }

    public User authenticateUser(String phoneNumber, String password) throws Exception {
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            } else {
                throw new Exception("Mật khẩu không đúng!");
            }
        } else {
            throw new Exception("Số điện thoại không tồn tại!");
        }
    }

    @Transactional
    public List<OrderDTO> getOrdersByUserId(UUID userId) {
        List<Orders> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private OrderDTO mapToDTO(Orders order) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        orderDTO.setOrderDate(order.getOrderDate());
        orderDTO.setTotalPrice(order.getTotalPrice());
        orderDTO.setStatus(order.getStatus().toString());
        orderDTO.setOrderItems(order.getOrderItems().stream().map(this::mapToItemDTO).collect(Collectors.toList()));
        return orderDTO;
    }

    private OrderItemDTO mapToItemDTO(OrderItem item) {
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setId(item.getId());
        itemDTO.setProductName(item.getProduct().getTenSanPham());
        itemDTO.setProductImageUrl(item.getProduct().getMainImageUrl()); // Đảm bảo gọi đúng phương thức
        itemDTO.setDonViTinh(item.getDonViTinh().getDonViTinh());
        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setPrice(item.getPrice());
        return itemDTO;
    }


}