package com.webanhang.team_project.dto.review;

import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class ReviewRequest {
    private Long productId;

    @Max(value = 500, message = "Content must be less than 500 characters")
    private String content;
}
