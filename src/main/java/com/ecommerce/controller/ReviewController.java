package com.ecommerce.controller;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;
import com.ecommerce.request.ReviewRequest;
import com.ecommerce.service.ReviewService;
import com.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Review> createReview(@RequestHeader("Authorization") String jwt, @RequestBody ReviewRequest reviewRequest) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        Review res = reviewService.createReview(user, reviewRequest);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getProductReview(@PathVariable Long productId) throws GlobalExceptionHandler {
        List<Review> res = reviewService.getReviewsByProductId(productId);
        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }
}
