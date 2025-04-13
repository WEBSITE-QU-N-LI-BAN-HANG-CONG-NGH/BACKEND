package com.webanhang.team_project.service;


import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.Rating;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.RatingRepository;
import com.webanhang.team_project.dto.review.RatingRequest;
import com.webanhang.team_project.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
    private final RatingRepository ratingRepository;
    private final ProductService productService;


    @Override
    public Rating createRating(RatingRequest ratingRequest, User user) {
        try {
            Product product = productService.findProductById(ratingRequest.getProductId());

            Rating rating = new Rating();
            rating.setRating(ratingRequest.getRating());
            rating.setProduct(product);
            rating.setUser(user);
            rating.setCreateAt(LocalDateTime.now());
            return ratingRepository.save(rating);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<Rating> getRatingsByProductId(Long productId) {
        return ratingRepository.findALlProductsRating(productId);
    }
}
