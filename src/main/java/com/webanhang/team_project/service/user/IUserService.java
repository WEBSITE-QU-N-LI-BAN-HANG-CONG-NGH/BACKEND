package com.webanhang.team_project.service.user;


import com.webanhang.team_project.dtos.UserDto;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.request.CreateUserRequest;
import com.webanhang.team_project.request.UpdateUserRequest;

public interface IUserService {
    User createUser(CreateUserRequest request);
    User updateUser(UpdateUserRequest request, int userId);
    User getUserById(int userId);
    void deleteUser(int userId);

    UserDto convertUserToDto(User user);
}
