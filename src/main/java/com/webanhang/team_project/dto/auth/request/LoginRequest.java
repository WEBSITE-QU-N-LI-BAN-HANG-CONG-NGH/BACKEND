package com.webanhang.team_project.dto.auth.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;


@Data
public class LoginRequest {

    @NotBlank(message = "Invalid login credentials")
    private String email;
    @NotBlank(message = "Invalid login credentials")
    private String password;
}
