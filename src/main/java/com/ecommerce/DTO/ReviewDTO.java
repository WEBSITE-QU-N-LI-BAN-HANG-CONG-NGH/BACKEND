package com.ecommerce.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private String review;
    private Long productId;
    private String userFirstName;
    private String userLastName;
    private LocalDateTime createdAt;
} 