package com.webanhang.team_project.controller.common;


import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.dto.user.request.CreateUserRequest;
import com.webanhang.team_project.dto.user.request.UpdateUserRequest;
import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/users")
public class UserController {
    private final IUserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        UserDTO userDto = userService.convertUserToDto(user);
        return ResponseEntity.ok(ApiResponse.success(userDto, "Found!"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        UserDTO userDto = userService.convertUserToDto(user);
        return ResponseEntity.ok(ApiResponse.success(userDto, "Create User Success!"));
    }

    @PutMapping("/{userId}/update")
    public ResponseEntity<ApiResponse> updateUser(@RequestBody UpdateUserRequest request, @PathVariable Long userId) {
        User user = userService.updateUser(request, userId);
        UserDTO userDto = userService.convertUserToDto(user);
        return ResponseEntity.ok(ApiResponse.success(userDto, "Update User Success!"));
    }

    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete User Success!"));
    }
}
