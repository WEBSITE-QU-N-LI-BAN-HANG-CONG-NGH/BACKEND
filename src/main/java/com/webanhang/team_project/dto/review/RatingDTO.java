package com.webanhang.team_project.dto.review;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RatingDTO {
    private Long id;
    private int rating;
    private Long productId;
    private String userFirstName;
    private String userLastName;
    private LocalDateTime createdAt;
} 