package com.webanhang.team_project.security.oauth2;

import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.Role;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.RoleRepository;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Xử lý thông tin người dùng sau khi đăng nhập OAuth2 thành công
 * Hỗ trợ đăng nhập qua Google và GitHub
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        OAuth2UserInfo userInfo;
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("OAuth2 authentication error", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        // Lấy provider name (google hoặc github)
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // Lấy thông tin email dựa theo provider
        String email = extractEmail(oauth2User.getAttributes(), provider);

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Tìm user trong DB, nếu chưa có thì tạo mới
        User user = userRepository.findByEmail(email);

        if (user == null) {
            user = this.createUser(oauth2User.getAttributes(), email, provider);
            log.info("Created new user from OAuth2: {}", email);
        } else {
            log.info("Existing user logged in via OAuth2: {}", email);

            // Nếu tài khoản chưa được kích hoạt thì kích hoạt
            if (!user.isActive()) {
                user.setActive(true);
                userRepository.save(user);
            }
        }

        return oauth2User;
    }

    private String extractEmail(Map<String, Object> attributes, String provider) {
        String email = null;

        if ("google".equals(provider)) {
            email = (String) attributes.get("email");
        } else if ("github".equals(provider)) {
            email = (String) attributes.get("email");

            // GitHub không luôn trả về email trong attributes cơ bản
            if (email == null) {
                // Trong trường hợp thực tế, bạn cần gọi GitHub API để lấy email
                // Ở đây chỉ là phần giả lập
                log.warn("Email not available in GitHub attributes. This would require an extra API call.");
            }
        }

        return email;
    }

    private User createUser(Map<String, Object> attributes, String email, String provider) {
        User user = new User();
        user.setEmail(email);
        user.setActive(true);

        // Set thông tin tên dựa vào provider
        if ("google".equals(provider)) {
            String name = (String) attributes.get("name");
            setNames(user, name);
        } else if ("github".equals(provider)) {
            String name = (String) attributes.get("name");
            if (name == null) {
                name = (String) attributes.get("login"); // Fallback to login name
            }
            setNames(user, name);
        }

        // Tạo random password
        String randomPassword = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));

        // Set role mặc định là CUSTOMER
        Role role = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRole(role);

        return userRepository.save(user);
    }

    private void setNames(User user, String fullName) {
        if (fullName != null && !fullName.isEmpty()) {
            String[] names = fullName.split(" ");
            if (names.length > 0) {
                user.setFirstName(names[0]);

                if (names.length > 1) {
                    StringBuilder lastName = new StringBuilder();
                    for (int i = 1; i < names.length; i++) {
                        lastName.append(names[i]).append(" ");
                    }
                    user.setLastName(lastName.toString().trim());
                } else {
                    user.setLastName("");
                }
            }
        } else {
            user.setFirstName("User");
            user.setLastName("");
        }
    }
}