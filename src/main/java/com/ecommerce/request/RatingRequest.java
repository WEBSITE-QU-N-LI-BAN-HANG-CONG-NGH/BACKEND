package com.ecommerce.request;
import lombok.Data;

@Data
public class RatingRequest {
    private Long productId;
    private int rating;
}
