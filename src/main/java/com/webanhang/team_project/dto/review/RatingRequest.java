package com.webanhang.team_project.dto.review;
import lombok.Data;

@Data
public class RatingRequest {
    private Long productId;
    private int rating;
}
