package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.user.UserDto;
import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.Role;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.RoleRepository;
import com.webanhang.team_project.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManageUserService implements IManageUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;

    @Override
    public Page<UserDto> getAllUsers(int page, int size, String search, String role) {
        Pageable pageable = PageRequest.of(page, size);

        // Filter logic
        List<User> users;
        if (StringUtils.hasText(search) && StringUtils.hasText(role)) {
            // Search by name/email and filter by role
            users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContainingAndRoleName(
                    search, search, search, role, pageable);
        } else if (StringUtils.hasText(search)) {
            // Only search
            users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                    search, search, search, pageable);
        } else if (StringUtils.hasText(role)) {
            // Only filter by role
            users = userRepository.findByRoleName(role, pageable);
        } else {
            // No filters
            users = userRepository.findAll(pageable).getContent();
        }

        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(userDtos, pageable, userRepository.count());
    }

    @Override
    public UserDto getUserDetails(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return convertToDto(user);
    }

    @Override
    @Transactional
    public UserDto changeUserRole(int userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
        }

        Role role = roleRepository.findByName(userRole)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        user.setRole(role);
        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUserStatus(int userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setActive(active);
        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userRepository.delete(user);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setActive(user.isActive());

        if (user.getRole() != null) {
            dto.setRole(user.getRole().getName().toString());
        }

        return dto;
    }
}
