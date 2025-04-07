package com.ecommerce.response;

import com.ecommerce.DTO.AddressDTO;
import com.ecommerce.DTO.CartDTO;
import com.ecommerce.DTO.OrderDTO;
import com.ecommerce.model.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String mobile;
    private String role;
    private List<AddressDTO> address = new ArrayList<>();
    private List<Rating> ratings = new ArrayList<>();
    private List<PaymentInformation> paymentInformation = new ArrayList<>();
    private List<OrderDTO> orders = new ArrayList<>();
    private List<Review> reviews = new ArrayList<>();
    private CartDTO cart;
    private LocalDateTime createdAt;
}

