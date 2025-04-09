package com.webanhang.team_project.service.user;

import com.webanhang.team_project.dto.AddAddressRequest;
import com.webanhang.team_project.dto.AddressDTO;
import com.webanhang.team_project.dto.user.UserDTO;
import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.Address;
import com.webanhang.team_project.model.Role;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.AddressRepository;
import com.webanhang.team_project.repository.RoleRepository;
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.dto.user.request.CreateUserRequest;
import com.webanhang.team_project.dto.auth.request.OtpVerificationRequest;
import com.webanhang.team_project.dto.auth.request.RegisterRequest;
import com.webanhang.team_project.dto.user.request.UpdateUserRequest;
import com.webanhang.team_project.security.jwt.JwtUtils;
import com.webanhang.team_project.security.otp.OtpService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AddressRepository addressRepository;
    private final JwtUtils jwtUtils;

    @Override
    public User createUser(CreateUserRequest request) {
        return Optional.of(request)
                .filter(user -> !userRepository.existsByEmail(request.getEmail()))
                .map(req -> {
                    User user = new User();
                    user.setFirstName(req.getFirstName());
                    user.setLastName(req.getLastName());
                    user.setEmail(req.getEmail());
                    user.setPassword(req.getPassword());
                    return userRepository.save(user);
                }).orElseThrow(() -> new EntityExistsException("Email " + request.getEmail() + " already be used"));
    }

    @Override
    public User updateUser(UpdateUserRequest request, Long userId) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.findById(userId).ifPresentOrElse(userRepository::delete, () -> {
            throw new EntityNotFoundException("User not found");
        });
    }

    @Transactional
    @Override
    public UserDTO convertUserToDto(User user) {
        return modelMapper.map(user, UserDTO.class);
    }

    @Transactional
    @Override
    public void registerUser(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(false);

        UserRole userRole = UserRole.valueOf(request.getRole());
        Role role = roleRepository.findByName(userRole)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRole(role);
        userRepository.save(user);

        // Tạo và gửi OTP
        String otp = otpService.generateOtp(request.getEmail());
        otpService.sendOtpEmail(request.getEmail(), otp);
    }

    @Override
    public boolean verifyOtp(OtpVerificationRequest request) {
        // Kiểm tra OTP và kích hoạt tài khoản nếu hợp lệ
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        return isValid;
    }

    @Override
    public UserDTO findUserProfileByJwt(String jwt) {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7); // Remove "Bearer " prefix
        }
        String email = jwtUtils.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);

        if(user == null) {
            throw new EntityNotFoundException("User not found " + email);
        }
        return convertUserToDto(user);
    }

    @Override
    public User findUserByJwt(String jwt)  {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7); // Remove "Bearer " prefix
        }
        String email = jwtUtils.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);

        if(user == null) {
            throw new EntityNotFoundException("User not found " + email);
        }
        return user;
    }

    @Override
    public AddressDTO addUserAddress(User user, AddAddressRequest request) {
        List<Address> address=user.getAddress();
        if(address==null){
            address=new ArrayList<>();
        }
        Address newAddress=new Address();
        newAddress.setFirstName(request.getFirstName());
        newAddress.setLastName(request.getLastName());
        newAddress.setStreetAddress(request.getStreetAddress());
        newAddress.setCity(request.getCity());
        newAddress.setState(request.getState());
        newAddress.setZipCode(request.getZipCode());
        newAddress.setMobile(request.getMobile());
        newAddress.setUser(user);
        address.add(newAddress);
        addressRepository.save(newAddress);
        return new AddressDTO(newAddress);
    }
}
