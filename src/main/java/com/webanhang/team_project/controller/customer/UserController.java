package com.webanhang.team_project.controller.customer;



import com.webanhang.team_project.dto.AddAddressRequest;
import com.webanhang.team_project.dto.AddressDTO;
import com.webanhang.team_project.dto.cart.CartDTO;
import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.dto.user.UserProfileResponse;
import com.webanhang.team_project.model.Address;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import com.webanhang.team_project.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);






}
