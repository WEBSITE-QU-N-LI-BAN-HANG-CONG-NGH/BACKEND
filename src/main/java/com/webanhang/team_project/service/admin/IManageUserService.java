package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.user.UserDTO;
import org.springframework.data.domain.Page;

public interface IManageUserService {
    Page<UserDTO> getAllUsers(int page, int size, String search, String role);

    UserDTO getUserDetails(Long userId);

    UserDTO changeUserRole(Long userId, String roleName);

    UserDTO updateUserStatus(Long userId, boolean active);

    void deleteUser(Long userId);
}
