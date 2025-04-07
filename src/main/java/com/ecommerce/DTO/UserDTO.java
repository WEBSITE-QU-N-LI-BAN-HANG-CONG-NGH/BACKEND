package com.ecommerce.DTO;

import com.ecommerce.model.Address;
import com.ecommerce.model.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String mobile;
    private List<Address> addresses;
    private LocalDateTime createdAt;

    public UserDTO(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.mobile = user.getMobile();
        this.addresses = user.getAddress();
        this.createdAt = user.getCreatedAt();
    }
} 