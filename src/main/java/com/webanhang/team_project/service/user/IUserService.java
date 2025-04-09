package com.webanhang.team_project.service.user;


import com.webanhang.team_project.dto.AddAddressRequest;
import com.webanhang.team_project.dto.AddressDTO;
import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.dto.user.request.CreateUserRequest;
import com.webanhang.team_project.dto.auth.request.OtpVerificationRequest;
import com.webanhang.team_project.dto.auth.request.RegisterRequest;
import com.webanhang.team_project.dto.user.request.UpdateUserRequest;

public interface IUserService {
    User createUser(CreateUserRequest request);
    User updateUser(UpdateUserRequest request, Long userId);
    User getUserById(Long userId);
    void deleteUser(Long userId);

    UserDTO convertUserToDto(User user);

    void registerUser(RegisterRequest request);
    boolean verifyOtp(OtpVerificationRequest request);

    User findUserByJwt(String jwt);
    UserDTO findUserProfileByJwt(String jwt);

    AddressDTO addUserAddress(User user, AddAddressRequest request);
}
