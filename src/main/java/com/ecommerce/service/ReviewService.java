package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;
import com.ecommerce.request.ReviewRequest;

import java.security.GeneralSecurityException;
import java.util.List;

public interface ReviewService {
    public Review createReview(User user, ReviewRequest reviewRequest) throws GlobalExceptionHandler;
    public List<Review> getReviewsByProductId(Long productId) throws GlobalExceptionHandler;
}
