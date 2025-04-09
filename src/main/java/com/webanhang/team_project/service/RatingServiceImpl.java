package com.webanhang.team_project.service;


import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.Rating;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.RatingRepository;
import com.webanhang.team_project.dto.RatingRequest;
import com.webanhang.team_project.service.product.ProductService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RatingServiceImpl implements RatingService {
    private RatingRepository ratingRepository;
    private ProductService productService;

    public RatingServiceImpl(RatingRepository ratingRepository, ProductService productService) {
        this.ratingRepository = ratingRepository;
        this.productService = productService;
    }

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
        } catch (GlobalExceptionHandler e) {
            throw new GlobalExceptionHandler(e.getMessage());
        }
    }

    @Override
    public List<Rating> getRatingsByProductId(Long productId) {
        return ratingRepository.findALlProductsRating(productId);
    }
}
