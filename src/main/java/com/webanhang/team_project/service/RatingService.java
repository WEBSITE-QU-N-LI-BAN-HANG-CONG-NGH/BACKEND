package com.webanhang.team_project.service;


import com.webanhang.team_project.model.Rating;
import com.webanhang.team_project.dto.RatingRequest;
import com.webanhang.team_project.model.User;

import java.util.List;

public interface RatingService {
    public Rating createRating(RatingRequest ratingRequest, User user);
    public List<Rating> getRatingsByProductId(Long productId);
}
