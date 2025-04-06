package com.webanhang.team_project.dto.user;

import lombok.Data;

@Data
public class UserDto {
    private int id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean active;
}

