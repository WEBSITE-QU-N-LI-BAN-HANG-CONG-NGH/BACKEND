package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Rating;
import com.ecommerce.model.User;
import com.ecommerce.request.RatingRequest;

import java.util.List;

public interface RatingService {
    public Rating createRating(RatingRequest ratingRequest, User user) throws GlobalExceptionHandler;
    public List<Rating> getRatingsByProductId(Long productId) throws GlobalExceptionHandler;
}
