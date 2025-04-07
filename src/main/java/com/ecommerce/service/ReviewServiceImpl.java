package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.request.ReviewRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService{

    private ReviewRepository reviewRepository;
    private ProductService productService;
    private ProductRepository productRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductService productService, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @Override
    public Review createReview(User user, ReviewRequest reviewRequest) throws GlobalExceptionHandler {
        try {
            Product product = productService.findProductById(reviewRequest.getProductId());

            Review review = new Review();
            review.setContent(reviewRequest.getContent());
            review.setProduct(product);
            review.setUser(user);
            review.setCreatedAt(LocalDateTime.now());

            return reviewRepository.save(review);
        } catch (GlobalExceptionHandler e) {
            throw new GlobalExceptionHandler(e.getMessage());
        }
    }

    @Override
    public List<Review> getReviewsByProductId(Long productId) throws GlobalExceptionHandler{
        return reviewRepository.findAllByProductId(productId);
    }
}
