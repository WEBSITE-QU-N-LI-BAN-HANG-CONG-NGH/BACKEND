package com.webanhang.team_project.controller.customer;


import com.webanhang.team_project.model.Review;
import com.webanhang.team_project.dto.review.ReviewRequest;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.ReviewService;
import com.webanhang.team_project.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Review> createReview(@RequestHeader("Authorization") String jwt, @RequestBody ReviewRequest reviewRequest) {
        User user = userService.findUserByJwt(jwt);
        Review res = reviewService.createReview(user, reviewRequest);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getProductReview(@PathVariable Long productId) {
        List<Review> res = reviewService.getReviewsByProductId(productId);
        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }
}
