package com.ecommerce.controller;


import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;
import com.ecommerce.request.RatingRequest;
import com.ecommerce.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rating")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Rating> createRating(@RequestHeader("Authorization") String jwt, @RequestBody RatingRequest ratingRequest) throws GlobalExceptionHandler {
        User user = userService.findUserByJwt(jwt);
        Rating res = ratingService.createRating(ratingRequest, user);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Rating>> getProductRating(@PathVariable Long productId) throws GlobalExceptionHandler {
        List<Rating> res = ratingService.getRatingsByProductId(productId);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
