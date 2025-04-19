package com.example.BnThuocOnline2025.service;

import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.model.UserAddress;
import com.example.BnThuocOnline2025.repository.UserAddressRepository;
import com.example.BnThuocOnline2025.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
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
        // Nếu địa chỉ được đặt làm mặc định, bỏ mặc định của các địa chỉ khác
        if (address.getIsDefault()) {
            List<UserAddress> defaultAddresses = userAddressRepository.findByUserAndIsDefault(address.getUser(), true);
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


}