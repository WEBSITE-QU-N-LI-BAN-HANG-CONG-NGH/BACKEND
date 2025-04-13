package com.webanhang.team_project.service;



import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.repository.ReviewRepository;
import com.webanhang.team_project.dto.ReviewRequest;
import com.webanhang.team_project.model.Review;
import com.webanhang.team_project.service.product.IProductService;
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
    public Review createReview(User user, ReviewRequest reviewRequest) {
        try {
            Product product = productService.findProductById(reviewRequest.getProductId());

            Review review = new Review();
            review.setContent(reviewRequest.getContent());
            review.setProduct(product);
            review.setUser(user);
            review.setCreatedAt(LocalDateTime.now());

            return reviewRepository.save(review);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<Review> getReviewsByProductId(Long productId){
        return reviewRepository.findAllByProductId(productId);
    }
}
