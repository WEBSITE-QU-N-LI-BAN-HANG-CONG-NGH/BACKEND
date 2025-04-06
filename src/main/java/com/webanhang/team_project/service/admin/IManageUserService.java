package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.user.UserDto;
import org.springframework.data.domain.Page;

public interface IManageUserService {
    Page<UserDto> getAllUsers(int page, int size, String search, String role);

    UserDto getUserDetails(int userId);

    UserDto changeUserRole(int userId, String roleName);

    UserDto updateUserStatus(int userId, boolean active);

    void deleteUser(int userId);
}
