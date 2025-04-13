package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.Role;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.OrderRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManageUserService implements IAdminManageUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final OrderRepository orderRepository;

    @Override
    public Page<UserDTO> getAllUsers(int page, int size, String search, String role) {
        Pageable pageable = PageRequest.of(page, size);

        // Filter logic
        List<User> users;
        if (StringUtils.hasText(search) && StringUtils.hasText(role)) {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            // Search by name/email and filter by role
            users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContainingAndRoleName(
                    search, search, search, userRole, pageable);
        } else if (StringUtils.hasText(search)) {
            // Only search
            users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                    search, search, search, pageable);
        } else if (StringUtils.hasText(role)) {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            // Only filter by role
            users = userRepository.findByRoleName(userRole, pageable);
        } else {
            // No filters
            users = userRepository.findAll(pageable).getContent();
        }

        List<UserDTO> userDTOS = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(userDTOS, pageable, userRepository.count());
    }

    @Override
    public UserDTO getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return convertToDto(user);
    }

    @Override
    @Transactional
    public UserDTO changeUserRole(Long userId, String roleName) {
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
    public UserDTO updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setActive(active);
        User savedUser = userRepository.save(user);

        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userRepository.delete(user);
    }

    private UserDTO convertToDto(User user) {
        UserDTO dto = new UserDTO();
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

    @Override
    public Map<String, Object> getCustomerStatistics() {
        Map<String, Object> result = new HashMap<>();

        // Lấy tất cả khách hàng (có role CUSTOMER)
        List<User> customers = userRepository.findByRoleName(UserRole.CUSTOMER, Pageable.unpaged());

        // Tổng số khách hàng
        result.put("totalCustomers", customers.size());

        // Tổng chi tiêu của khách hàng
        BigDecimal totalSpending = BigDecimal.ZERO;

        for (User customer : customers) {
            List<Order> customerOrders = orderRepository.findByUserId(customer.getId());

            BigDecimal customerSpending = customerOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                    .map(order -> BigDecimal.valueOf(order.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSpending = totalSpending.add(customerSpending);
        }

        result.put("totalSpending", totalSpending);

        // Khách hàng VIP (có tổng chi tiêu cao nhất)
        int vipCount = Math.min(customers.size(), 2); // Lấy 2 khách hàng chi tiêu cao nhất

        // Sắp xếp khách hàng theo chi tiêu
        List<User> sortedCustomers = new ArrayList<>(customers);
        sortedCustomers.sort((c1, c2) -> {
            BigDecimal spending1 = calculateCustomerSpending(c1.getId());
            BigDecimal spending2 = calculateCustomerSpending(c2.getId());
            return spending2.compareTo(spending1); // Giảm dần
        });

        result.put("vipCustomers", vipCount);

        // Số lượng đơn hàng trung bình
        int totalOrders = 0;
        for (User customer : customers) {
            List<Order> customerOrders = orderRepository.findByUserId(customer.getId());
            totalOrders += customerOrders.size();
        }

        double avgOrders = customers.isEmpty() ? 0 : (double) totalOrders / customers.size();
        result.put("averageOrders", avgOrders);

        return result;
    }

    private BigDecimal calculateCustomerSpending(Long customerId) {
        List<Order> customerOrders = orderRepository.findByUserId(customerId);

        return customerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                .map(order -> BigDecimal.valueOf(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
