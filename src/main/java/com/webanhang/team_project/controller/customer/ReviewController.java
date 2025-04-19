package com.webanhang.team_project.controller.customer;


import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.review.ReviewDTO;
import com.webanhang.team_project.model.Review;
import com.webanhang.team_project.dto.review.ReviewRequest;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.review.ReviewService;
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
    public ResponseEntity<ApiResponse> createReview(@RequestHeader("Authorization") String jwt, @RequestBody ReviewRequest reviewRequest) {
        if (jwt == null || jwt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        User user = userService.findUserByJwt(jwt);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Review res = reviewService.createReview(user, reviewRequest);
        if (res == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        ReviewDTO reviewDTO = new ReviewDTO(res);
        return ResponseEntity.ok(ApiResponse.success(reviewDTO, "Create Review Success!"));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse> getProductReview(@PathVariable Long productId) {
        List<Review> res = reviewService.getReviewsByProductId(productId);
        if (res == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<ReviewDTO> reviewDTOs = res.stream()
                .map(review -> new ReviewDTO(review))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(reviewDTOs, "Create Review By Product Success!"));
    }

    @PutMapping("/update/{reviewId}")
    public ResponseEntity<ApiResponse> updateReview(@PathVariable Long reviewId, @RequestBody ReviewRequest reviewRequest) {
        Review res = reviewService.updateReview(reviewId, reviewRequest);

        if (res == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        ReviewDTO reviewDTO = new ReviewDTO(res);
        return ResponseEntity.ok(ApiResponse.success(reviewDTO, "Update Review Success!"));
    }

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete Review Success!"));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> getReviewById(@PathVariable Long reviewId) {
        Review res = reviewService.getReviewById(reviewId);
        if (res == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        ReviewDTO reviewDTO = new ReviewDTO(res);
        return ResponseEntity.ok(ApiResponse.success(reviewDTO, "Get Review By Id Success!"));
    }
}
