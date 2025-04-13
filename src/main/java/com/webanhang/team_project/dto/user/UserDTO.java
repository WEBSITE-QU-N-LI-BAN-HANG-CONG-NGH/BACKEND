package com.webanhang.team_project.dto.user;

import com.webanhang.team_project.model.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String mobile;
    private boolean active;
    private List<Address> addresses;
    private LocalDateTime createdAt;
}

