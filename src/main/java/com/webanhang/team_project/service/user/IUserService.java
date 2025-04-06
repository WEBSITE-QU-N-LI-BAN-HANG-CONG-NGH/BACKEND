package com.webanhang.team_project.service.user;


import com.webanhang.team_project.dto.user.UserDto;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.dto.user.request.CreateUserRequest;
import com.webanhang.team_project.dto.auth.request.OtpVerificationRequest;
import com.webanhang.team_project.dto.auth.request.RegisterRequest;
import com.webanhang.team_project.dto.user.request.UpdateUserRequest;

public interface IUserService {
    User createUser(CreateUserRequest request);
    User updateUser(UpdateUserRequest request, int userId);
    User getUserById(int userId);
    void deleteUser(int userId);

    UserDto convertUserToDto(User user);
    void registerUser(RegisterRequest request);
    boolean verifyOtp(OtpVerificationRequest request);
}
