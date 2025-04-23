package com.webanhang.team_project.controller.common;


import com.webanhang.team_project.dto.address.AddAddressRequest;
import com.webanhang.team_project.dto.address.AddressDTO;
import com.webanhang.team_project.dto.cart.CartDTO;
import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.dto.user.UserProfileResponse;
import com.webanhang.team_project.model.Address;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.dto.user.CreateUserRequest;
import com.webanhang.team_project.dto.user.UpdateUserRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/users")
@Slf4j
public class UserController {
    private final IUserService userService;
    private final UserRepository userRepository;


//    @GetMapping("/{userId}")
//    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
//        User user = userService.getUserById(userId);
//        UserDTO userDto = userService.convertUserToDto(user);
//        return ResponseEntity.ok(ApiResponse.success(userDto, "Found!"));
//    }

//    @PostMapping("/add")
//    public ResponseEntity<ApiResponse> createUser(@RequestBody CreateUserRequest request) {
//        User user = userService.createUser(request);
//        UserDTO userDto = userService.convertUserToDto(user);
//        return ResponseEntity.ok(ApiResponse.success(userDto, "Create User Success!"));
//    }

    @PutMapping("/{userId}/update")
    public ResponseEntity<ApiResponse> updateUser(@RequestBody UpdateUserRequest request, @PathVariable Long userId) {
        User user = userService.updateUser(request, userId);
        UserDTO userDto = userService.convertUserToDto(user);
        return ResponseEntity.ok(ApiResponse.success(userDto, "Update User Success!"));
    }

    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete User Success!"));
    }

    @Transactional
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                log.error("Authentication failed - auth object is null or name is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication failed", "code", "AUTH_ERROR"));
            }

            String email = authentication.getName();
            log.info("Getting profile for user with email: {}", email);

            User user = userRepository.findByEmail(email);
            if (user == null) {
                log.error("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found for email: " + email, "code", "USER_NOT_FOUND"));
            }

            log.info("User found with ID: {}", user.getId());

            // Kiểm tra danh sách địa chỉ, nếu null thì tạo danh sách rỗng
            List<AddressDTO> addressDTOS = new ArrayList<>();
            if (user.getAddress() != null) {
                for (Address address : user.getAddress()) {
                    if (address != null) {
                        addressDTOS.add(new AddressDTO(address));
                    }
                }
            }

            // Kiểm tra danh sách đơn hàng
            List<OrderDTO> orderDTOS = new ArrayList<>();
            if (user.getOrders() != null) {
                for (Order order : user.getOrders()) {
                    if (order != null) {
                        try {
                            OrderDTO orderDTO = new OrderDTO(order);
                            orderDTOS.add(orderDTO);
                        } catch (Exception ex) {
                            log.warn("Error converting order to DTO: " + ex.getMessage());
                            // Tiếp tục với đơn hàng tiếp theo thay vì dừng toàn bộ quá trình
                        }
                    }
                }
            }

            // Xử lý response
            UserProfileResponse profileResponse = new UserProfileResponse();

            ///   check status
            if (authentication != null && authentication.isAuthenticated()) {
                log.info("User is authenticated");
                profileResponse.setStatus(true);
            } else {
                log.warn("User is not authenticated");
                profileResponse.setStatus(false);
            }

            profileResponse.setId(user.getId());
            profileResponse.setEmail(user.getEmail());
            profileResponse.setFirstName(user.getFirstName());
            profileResponse.setLastName(user.getLastName());
            profileResponse.setMobile(user.getPhone());
            profileResponse.setRole(user.getRole() != null && user.getRole().getName() != null ?
                    user.getRole().getName().name() : "UNKNOWN");
            profileResponse.setAddress(addressDTOS);
            // Kiểm tra cart null
            profileResponse.setCart(user.getCart() != null ? new CartDTO(user.getCart()) : null);
            profileResponse.setCreatedAt(user.getCreatedAt());
            profileResponse.setOrders(orderDTOS);
            profileResponse.setImageUrl(user.getImageUrl());
            profileResponse.setOauthProvider(user.getOauthProvider());

            log.info("Successfully retrieved profile for user: {}", email);
            return ResponseEntity.ok(profileResponse);
        } catch (Exception e) {
            log.error("Error getting user profile: ", e);
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
            log.error("Error getting user address: ", e);
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
