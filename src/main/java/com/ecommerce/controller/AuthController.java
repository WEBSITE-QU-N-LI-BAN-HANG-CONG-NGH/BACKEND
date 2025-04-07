package com.ecommerce.controller;

import com.ecommerce.config.JwtProvider;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Address;
import com.ecommerce.model.Cart;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.request.LoginRequest;
import com.ecommerce.response.AuthResponse;
import com.ecommerce.service.CartService;
import com.ecommerce.service.CustomerUserServiceImplementation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController // Đánh dấu lớp là controller trả về JSON.
@RequestMapping("/auth")
public class AuthController {

    private UserRepository userRepository;
    private JwtProvider jwtProvider;
    private PasswordEncoder passwordEncoder;
    private CustomerUserServiceImplementation customerUserServiceImplementation;
    private CartService cartService;

    @Autowired
    public AuthController(UserRepository userRepository, JwtProvider jwtProvider,
           PasswordEncoder passwordEncoder, CustomerUserServiceImplementation customerUserServiceImplementation, CartService cartService) {
        this.userRepository = userRepository;
        this.jwtProvider=jwtProvider;
        this.passwordEncoder=passwordEncoder;
        this.customerUserServiceImplementation=customerUserServiceImplementation;
        this.cartService=cartService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> createUserHandler(@RequestBody @Valid User user) {
        String email = user.getEmail();
        String password = user.getPassword();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String mobile = user.getMobile();
        String role = user.getRole();
        List<Address> addresses = user.getAddress();

        User isUserExist = userRepository.findByEmail(email);

        if (isUserExist != null) {
            throw new GlobalExceptionHandler("Email is already used with another account", "USER_EXISTS");
        }

        // Tạo và lưu user trước
        User createdUser = new User();
        createdUser.setEmail(email);
        createdUser.setPassword(passwordEncoder.encode(password));
        createdUser.setFirstName(firstName);
        createdUser.setLastName(lastName);
        createdUser.setMobile(mobile);
        createdUser.setRole(role);
        
        User savedUser = userRepository.save(createdUser);

        // Sau khi có user đã lưu, thiết lập quan hệ với address
        if (addresses != null) {
            for (Address address : addresses) {
                address.setUser(savedUser);
            }
            savedUser.setAddress(addresses);
            savedUser = userRepository.save(savedUser);
        }

        // Tạo cart cho user mới
        Cart cart = cartService.createCart(savedUser);
        savedUser.setCart(cart);
        userRepository.save(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(savedUser.getEmail(), null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("User registered successfully");

        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUserHandler(@RequestBody @Valid LoginRequest loginRequest) {
        String username = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        Authentication authentication=authenticate(username, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token =jwtProvider.generateToken(authentication);
        AuthResponse authResponse=new AuthResponse(token, "User logged in successfully");

        return new ResponseEntity<AuthResponse>(authResponse, HttpStatus.OK);
    }

    private Authentication authenticate(String username, String password) {
        UserDetails userDetails= customerUserServiceImplementation.loadUserByUsername(username);

        if(userDetails==null) {
            throw new BadCredentialsException("User not found");
        }

        if(!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

}







/*
@Valid: Đây là một annotation từ gói jakarta.validation, được sử dụng để kích hoạt validation (kiểm tra tính hợp lệ) của dữ liệu đầu vào (User hoặc LoginRequest) trước khi xử lý trong controller.
Validation không áp dụng trực tiếp cho AuthResponse: @Valid được sử dụng cho dữ liệu đầu vào (input), không phải dữ liệu đầu ra như AuthResponse. Trong trường hợp này, User và LoginRequest có các annotation như @NotBlank, @Email, @Size, v.v., để đảm bảo dữ liệu đầu vào hợp lệ trước khi xử lý.




DTO (Data Transfer Object) là một mẫu thiết kế (design pattern) trong lập trình, được sử dụng để truyền dữ liệu giữa các tầng (layers) hoặc hệ thống khác nhau trong ứng dụng. Trong ngữ cảnh của ứng dụng Java (như ứng dụng Spring Boot của bạn), DTO thường được dùng để:

Truyền dữ liệu giữa client và server: DTO định dạng dữ liệu gửi từ client (request) hoặc trả về cho client (response).
Giảm sự phụ thuộc vào entity: Thay vì trả về trực tiếp các entity (như User), DTO giúp kiểm soát dữ liệu được gửi đi, tránh lộ các thông tin không cần thiết.
Tối ưu hóa dữ liệu: DTO có thể chỉ chứa các trường cần thiết, giảm kích thước dữ liệu truyền tải.
Ví dụ về DTO trong mã nguồn của bạn:
LoginRequest (DTO cho request): Nhận dữ liệu đăng nhập từ client.
AuthResponse (DTO cho response): Trả về token và thông điệp cho client.
Tại sao AuthResponse là một DTO?
Chỉ chứa dữ liệu cần thiết: AuthResponse chỉ chứa jwt và message, không bao gồm các thông tin không cần thiết từ entity User (như password, address, v.v.).
Tách biệt tầng logic: AuthResponse không phụ thuộc trực tiếp vào entity User, giúp tách biệt tầng controller và tầng model.
Dễ serialize thành JSON: Cấu trúc đơn giản của AuthResponse phù hợp để chuyển đổi thành JSON và gửi cho client.
So sánh DTO và Entity:
Entity (User): Đại diện cho bảng trong cơ sở dữ liệu, chứa toàn bộ thông tin của một người dùng (bao gồm các trường nhạy cảm như password).
DTO (AuthR
 */