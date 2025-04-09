package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.user.UserDTO;
import org.springframework.data.domain.Page;

public interface IManageUserService {
    Page<UserDTO> getAllUsers(int page, int size, String search, String role);

    UserDTO getUserDetails(int userId);

    UserDTO changeUserRole(int userId, String roleName);

    UserDTO updateUserStatus(int userId, boolean active);

    void deleteUser(int userId);
}
