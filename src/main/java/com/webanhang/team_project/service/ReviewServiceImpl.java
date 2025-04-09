package com.webanhang.team_project.service;


import com.ecommerce.service.IProductService;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.repository.ReviewRepository;
import com.webanhang.team_project.dto.ReviewRequest;
import com.webanhang.team_project.model.Review;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService{

    private ReviewRepository reviewRepository;
    private IProductService productService;
    private ProductRepository productRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, IProductService productService, ProductRepository productRepository) {
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
