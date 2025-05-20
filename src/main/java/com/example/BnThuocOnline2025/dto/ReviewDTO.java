package com.example.BnThuocOnline2025.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private int id;
    private LocalDateTime created_at;
    private float rating;
    private String review_content;
    private LocalDateTime updated_at;
    private int product_id;
    private String product_name;
    private UUID user_id;
    private String username;
    private boolean approved;
    private String reply;
}
