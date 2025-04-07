package com.ecommerce.controller;

import com.ecommerce.DTO.*;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.*;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.request.AddAddressRequest;
import com.ecommerce.response.UserProfileResponse;
import com.ecommerce.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                logger.error("Authentication failed - auth object is null or name is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication failed", "code", "AUTH_ERROR"));
            }

            String email = authentication.getName();
            logger.info("Getting profile for user with email: {}", email);
            
            User user = userRepository.findByEmail(email);
            if (user == null) {
                logger.error("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for email: " + email, "code", "USER_NOT_FOUND"));
            }

            logger.info("User found with ID: {}", user.getId());
            
            // Kiểm tra danh sách địa chỉ, nếu null thì tạo danh sách rỗng
            List<AddressDTO> addressDTOS = new ArrayList<>();
            if (user.getAddress() != null) {
                for (Address address : user.getAddress()) {
                    addressDTOS.add(new AddressDTO(address));
                }
            }

            // Kiểm tra danh sách đơn hàng
            List<OrderDTO> orderDTOS = new ArrayList<>();
            if (user.getOrders() != null) {
                for (Order order : user.getOrders()) {
                    orderDTOS.add(new OrderDTO(order));
                }
            }

            // Xử lý response
            UserProfileResponse profileResponse = new UserProfileResponse();
            profileResponse.setId(user.getId());
            profileResponse.setEmail(user.getEmail());
            profileResponse.setFirstName(user.getFirstName());
            profileResponse.setLastName(user.getLastName());
            profileResponse.setMobile(user.getMobile());
            profileResponse.setRole(user.getRole());
            profileResponse.setAddress(addressDTOS);
            profileResponse.setPaymentInformation(user.getPaymentInformation() != null ? user.getPaymentInformation() : new ArrayList<>());
            profileResponse.setRatings(user.getRatings() != null ? user.getRatings() : new ArrayList<>());
            profileResponse.setCart(user.getCart() != null ? new CartDTO(user.getCart()) : null);
            profileResponse.setCreatedAt(user.getCreatedAt());
            profileResponse.setOrders(orderDTOS);

            logger.info("Successfully retrieved profile for user: {}", email);
            return ResponseEntity.ok(profileResponse);
        } catch (Exception e) {
            logger.error("Error getting user profile: ", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage(), "code", "INTERNAL_ERROR"));
        }
    }



    @GetMapping("/address")
    public ResponseEntity<?> getUserAddress() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication failed", "code", "AUTH_ERROR"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email);

            if(user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for email: " + email, "code", "USER_NOT_FOUND"));
            }

            List<AddressDTO> addressDTOS = new ArrayList<>();

            if(user.getAddress() != null) {
                for (Address a: user.getAddress()) {
                    addressDTOS.add(new AddressDTO(a));
                }
            }

            return ResponseEntity.ok(addressDTOS);
        } catch (Exception e) {
            logger.error("Error getting user address: ", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage(), "code", "INTERNAL_ERROR"));
        }
    }


    @PostMapping("/addresses")
    public ResponseEntity<?> addUserAddress(@RequestHeader("Authorization") String jwt,
                                            @RequestBody AddAddressRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed", "code", "AUTH_ERROR"));
        }
        String email = authentication.getName();
        User user= userRepository.findByEmail(email);
        if(user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found for email: " + jwt, "code", "USER_NOT_FOUND"));
        }

        userService.addUserAddress(user, req);
        return ResponseEntity.ok(Map.of("message", "Address added successfully"));
    }


}
