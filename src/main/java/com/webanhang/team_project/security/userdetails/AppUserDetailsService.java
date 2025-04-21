package com.webanhang.team_project.security.userdetails;

import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
// import jakarta.persistence.EntityNotFoundException; // Không thấy dùng
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException, DisabledException {
        // Sử dụng findByEmail trực tiếp, Optional chỉ làm phức tạp hơn ở đây
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        if (!user.isActive()) {
            // Ném DisabledException thay vì trả về UserDetails với isEnabled=false
            // Để có thông báo lỗi rõ ràng hơn cho người dùng
            throw new DisabledException("Account is not activated for email: " + email);
        }

        // Sử dụng factory method chuẩn cho UserDetails
        return AppUserDetails.buildUserDetails(user);
    }
}