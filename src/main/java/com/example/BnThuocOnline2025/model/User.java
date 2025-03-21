package com.example.BnThuocOnline2025.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String facebookId;
    private String googleId;
    private String name;
    private String picture;
    private LocalDate dateOfBirth;
    private String address;
    private String password;
    private String phoneNumber;
    private String email;
    private String role; // Thêm trường role (ví dụ: "ADMIN" hoặc "USER")

    public User(String facebookId, String googleId, String name, String picture, String email) {
        this.facebookId = facebookId;
        this.googleId = googleId;
        this.name = name;
        this.picture = picture;
        this.email = email;
        this.role = "USER"; // Mặc định là USER
    }
}