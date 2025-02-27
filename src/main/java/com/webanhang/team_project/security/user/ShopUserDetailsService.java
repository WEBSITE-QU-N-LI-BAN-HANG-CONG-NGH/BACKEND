package com.webanhang.team_project.security.user;

import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShopUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = Optional.ofNullable(userRepository
                .findByEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return ShopUserDetails.buildUserDetails(user);
    }
}
