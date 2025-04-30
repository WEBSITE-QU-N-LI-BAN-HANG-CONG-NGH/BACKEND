package com.webanhang.team_project.service.admin;

import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.Cart;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.Role;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.*;
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

@Service
@RequiredArgsConstructor
public class AdminManageUserService implements IAdminManageUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentDetailRepository paymentDetailRepository;

    @Override
    public Page<UserDTO> getAllUsers(int page, int size, String search, String role) {
        Pageable pageable = PageRequest.of(page, size);

        // Filter logic
        List<User> users;

        // Thêm điều kiện không bao gồm ADMIN trong kết quả
        if (StringUtils.hasText(search) && StringUtils.hasText(role)) {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            // Search by name/email and filter by role
            users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContainingAndRoleName(
                    search, search, search, userRole, pageable);
        } else if (StringUtils.hasText(search)) {
            // Only search
            users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContainingAndRoleNameNot(
                    search, search, search, UserRole.ADMIN, pageable);
        } else if (StringUtils.hasText(role)) {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            // Only filter by role
            users = userRepository.findByRoleName(userRole, pageable);
        } else {
            // No filters - nhưng vẫn loại trừ ADMIN
            users = userRepository.findByRoleNameNot(UserRole.ADMIN, pageable);
        }

        List<UserDTO> userDTOS = users.stream()
                .map(this::convertToDto)
                .toList();

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

            // Xóa cart items trước khi xóa cart
            if (user.getCart() != null) {
                // Sử dụng clear() để xóa các cartItems được liên kết
                user.getCart().getCartItems().clear();
                cartRepository.delete(user.getCart());
            }
            orderItemRepository.deleteByOrderUserId(userId);
            orderRepository.deleteByUserId(userId);
            addressRepository.deleteByUserId(userId);
            reviewRepository.deleteByUserId(userId);
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
        // Tổng chi tiêu của khách hàng
        BigDecimal totalSpending = BigDecimal.ZERO;

        for (User customer : customers) {
            List<Order> customerOrders = orderRepository.findByUserId(customer.getId());

            BigDecimal customerSpending = customerOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                    .map(order -> BigDecimal.valueOf(order.getTotalDiscountedPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSpending = totalSpending.add(customerSpending);
        }

        // Sắp xếp khách hàng theo chi tiêu
        List<User> sortedCustomers = new ArrayList<>(customers);
        sortedCustomers.sort((c1, c2) -> {
            BigDecimal spending1 = calculateCustomerSpending(c1.getId());
            BigDecimal spending2 = calculateCustomerSpending(c2.getId());
            return spending2.compareTo(spending1); // Giảm dần
        });

        // Số lượng đơn hàng trung bình
        int totalOrders = 0;
        for (User customer : customers) {
            List<Order> customerOrders = orderRepository.findByUserId(customer.getId());
            totalOrders += customerOrders.size();
        }

        double avgOrders = customers.isEmpty() ? 0 : (double) totalOrders / customers.size();

        result.put("totalCustomers", customers.size());
        result.put("totalSpending", totalSpending);
        result.put("averageOrders", avgOrders);

        return result;
    }

    private BigDecimal calculateCustomerSpending(Long customerId) {
        List<Order> customerOrders = orderRepository.findByUserId(customerId);

        return customerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                .map(order -> BigDecimal.valueOf(order.getTotalDiscountedPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
