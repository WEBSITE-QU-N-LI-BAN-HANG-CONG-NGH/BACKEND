package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Product;
import com.ecommerce.model.Rating;
import com.ecommerce.model.User;
import com.ecommerce.repository.RatingRepository;
import com.ecommerce.request.RatingRequest;
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
    public Rating createRating(RatingRequest ratingRequest, User user) throws GlobalExceptionHandler {
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
