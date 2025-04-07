package com.ecommerce.service;

import com.ecommerce.DTO.AddressDTO;
import com.ecommerce.DTO.UserDTO;
import com.ecommerce.config.JwtProvider;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Address;
import com.ecommerce.model.User;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.request.AddAddressRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private UserRepository userRepository;
    private JwtProvider jwtProvider;
    private AddressRepository addressRepository;

    public UserServiceImpl(UserRepository userRepository, JwtProvider jwtProvider, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.addressRepository = addressRepository;
    }

    @Override
    public User findUserById(Long id) throws GlobalExceptionHandler {
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            return user.get();
        }
        throw new GlobalExceptionHandler("User not found", "USER_NOT_FOUND");
    }

    @Override
    public UserDTO findUserProfileByJwt(String jwt) throws GlobalExceptionHandler {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7); // Remove "Bearer " prefix
        }
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);

        if(user == null) {
            throw new GlobalExceptionHandler("User not found " + email, "USER_NOT_FOUND");
        }
        return new UserDTO(user);
    }

    @Override
    public User findUserByJwt(String jwt) throws GlobalExceptionHandler {
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7); // Remove "Bearer " prefix
        }
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);

        if(user == null) {
            throw new GlobalExceptionHandler("User not found " + email, "USER_NOT_FOUND");
        }
        return user;
    }

    @Override
    public AddressDTO addUserAddress(User user, AddAddressRequest request) throws GlobalExceptionHandler {
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
