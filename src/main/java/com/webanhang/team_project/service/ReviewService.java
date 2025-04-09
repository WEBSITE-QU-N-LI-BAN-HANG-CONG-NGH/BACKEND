package com.webanhang.team_project.service;


import com.webanhang.team_project.dto.ReviewRequest;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Review;
import com.webanhang.team_project.model.User;

import java.util.List;

public interface ReviewService {
    public Review createReview(User user, ReviewRequest reviewRequest);
    public List<Review> getReviewsByProductId(Long productId);
}
