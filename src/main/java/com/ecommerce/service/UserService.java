package com.ecommerce.service;

import com.ecommerce.DTO.AddressDTO;
import com.ecommerce.DTO.UserDTO;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.User;
import com.ecommerce.request.AddAddressRequest;

public interface UserService {

    public User findUserById(Long id) throws GlobalExceptionHandler;

    public UserDTO findUserProfileByJwt(String jwt) throws GlobalExceptionHandler;

    public User findUserByJwt(String jwt) throws GlobalExceptionHandler;

    public AddressDTO addUserAddress(User user, AddAddressRequest request) throws GlobalExceptionHandler;
}
