package com.example.BnThuocOnline2025.repository;

import com.example.BnThuocOnline2025.model.User;
import com.example.BnThuocOnline2025.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser(User user);
    List<UserAddress> findAllByUserAndIsDefault(User user, Boolean isDefault); // Đổi tên
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(UUID userId);
    Optional<UserAddress> findByUserAndIsDefault(User user, Boolean isDefault);
}