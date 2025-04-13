package com.webanhang.team_project.controller.customer;


import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Rating;
import com.webanhang.team_project.dto.RatingRequest;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.RatingService;
import com.webanhang.team_project.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("${api.prefix}/rating")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Rating> createRating(@RequestHeader("Authorization") String jwt, @RequestBody RatingRequest ratingRequest)  {
        User user = userService.findUserByJwt(jwt);
        Rating res = ratingService.createRating(ratingRequest, user);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Rating>> getProductRating(@PathVariable Long productId) {
        List<Rating> res = ratingService.getRatingsByProductId(productId);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
