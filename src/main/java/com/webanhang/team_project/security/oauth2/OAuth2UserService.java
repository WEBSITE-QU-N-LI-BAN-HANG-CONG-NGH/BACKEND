package com.webanhang.team_project.security.oauth2;

import com.webanhang.team_project.enums.UserRole;
import com.webanhang.team_project.model.Role;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.RoleRepository;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference; // Thêm import này
import org.springframework.http.HttpEntity; // Thêm import này
import org.springframework.http.HttpHeaders; // Thêm import này
import org.springframework.http.HttpMethod; // Thêm import này
import org.springframework.http.ResponseEntity; // Thêm import này
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken; // Thêm import này
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.webanhang.team_project.security.userdetails.AppUserDetails;// Thêm import này

import java.util.List; // Thêm import này
import java.util.Map;
import java.util.Optional; // Thêm import này
import java.util.UUID;

@Service
@RequiredArgsConstructor // Giữ nguyên nếu bạn dùng Lombok và final fields
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate; // ** Thêm RestTemplate **

    // Define standard error codes
    private static final String OAUTH2_PROCESSING_ERROR_CODE = "oauth2_processing_error";
    private static final String EMAIL_NOT_FOUND_ERROR_CODE = "email_not_found";
    private static final String ROLE_NOT_FOUND_ERROR_CODE = "role_not_found";
    private static final String GITHUB_EMAILS_API_URL = "https://api.github.com/user/emails"; // URL API emails của GitHub

    // ** Thêm Bean RestTemplate vào Configuration của bạn nếu chưa có **
    // Ví dụ trong lớp @Configuration chính:
    // @Bean
    // public RestTemplate restTemplate() {
    //     return new RestTemplate();
    // }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // ** Thay đổi kiểu trả về của phương thức này ngầm định là OAuth2User,
        //    nhưng chúng ta sẽ trả về AppUserDetails (vẫn là hợp lệ vì AppUserDetails implements OAuth2User)
        //    hoặc cần điều chỉnh để trả về UserDetails nếu cần thiết hơn trong ngữ cảnh Spring Security chung.
        //    Tuy nhiên, để đơn giản, trả về AppUserDetails hoạt động được với OAuth2SuccessHandler.
        // **

        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User); // processOAuth2User sẽ trả về AppUserDetails
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during OAuth2 user processing for provider {}: {}",
                    userRequest.getClientRegistration().getRegistrationId(), ex.getMessage(), ex);
            OAuth2Error error = new OAuth2Error(
                    OAUTH2_PROCESSING_ERROR_CODE,
                    "An unexpected error occurred processing the OAuth2 user: " + ex.getMessage(),
                    null
            );
            throw new OAuth2AuthenticationException(error, ex.getMessage(), ex);
        }
    }

    // ** Sửa đổi kiểu trả về thành AppUserDetails **
    private AppUserDetails processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = extractEmail(oauth2User.getAttributes(), provider, userRequest);

        if (!StringUtils.hasText(email)) {
            log.error("Email not found from OAuth2 provider: {}", provider);
            OAuth2Error error = new OAuth2Error(
                    EMAIL_NOT_FOUND_ERROR_CODE,
                    "Could not get email from OAuth2 provider (" + provider + "). Check provider configuration or user's email settings.",
                    null
            );
            throw new OAuth2AuthenticationException(error, "Email not found from OAuth2 provider");
        }

        User user = userRepository.findByEmail(email);

        if (user == null) {
            // ** Truyền cả attributes vào createUser để lấy thêm thông tin như name, image **
            user = this.createUser(oauth2User.getAttributes(), email, provider);
            log.info("Created new user from OAuth2 provider {}: {}", provider, email);
        } else {
            log.info("Existing user logged in via OAuth2 provider {}: {}", provider, email);
            if (!user.isActive()) {
                user.setActive(true);
                userRepository.save(user);
                log.info("Activated existing inactive user: {}", email);
            }
            // Optional: Update user details (name, image) from provider if needed
            // updateUserIfNeeded(user, oauth2User.getAttributes(), provider);
        }

        // ** Tạo và trả về AppUserDetails thay vì oauth2User gốc **
        return AppUserDetails.buildUserDetails(user);
    }

    // ** Sửa đổi để nhận userRequest và gọi API phụ **
    private String extractEmail(Map<String, Object> attributes, String provider, OAuth2UserRequest userRequest) {
        String email = null;

        if ("google".equalsIgnoreCase(provider)) {
            email = (String) attributes.get("email");
        } else if ("github".equalsIgnoreCase(provider)) {
            email = (String) attributes.get("email"); // Thử lấy email từ attributes chính trước

            // Nếu không có email trong attributes chính, và provider là github -> gọi API /user/emails
            if (!StringUtils.hasText(email)) {
                log.warn("Primary email attribute is null/empty for GitHub user [ID: {}]. Attempting to fetch from /user/emails endpoint.", attributes.get("id"));
                email = fetchGithubPrimaryVerifiedEmail(userRequest.getAccessToken());
            }
        }

        return email;
    }

    // ** Phương thức mới để gọi API /user/emails của GitHub **
    private String fetchGithubPrimaryVerifiedEmail(OAuth2AccessToken accessToken) {
        HttpHeaders headers = new HttpHeaders();
        // Gửi Access Token trong header Authorization
        headers.setBearerAuth(accessToken.getTokenValue());
        // Đặt header Accept theo khuyến nghị của GitHub API v3
        headers.add("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>("", headers);

        try {
            // Thực hiện gọi GET đến API emails
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    GITHUB_EMAILS_API_URL,
                    HttpMethod.GET,
                    entity,
                    // Định nghĩa kiểu trả về là một List các Map
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> emails = response.getBody();
                // Tìm email nào là primary VÀ verified
                Optional<String> primaryVerifiedEmail = emails.stream()
                        .filter(emailMap -> Boolean.TRUE.equals(emailMap.get("primary")) && Boolean.TRUE.equals(emailMap.get("verified")))
                        .map(emailMap -> (String) emailMap.get("email"))
                        .findFirst();

                if (primaryVerifiedEmail.isPresent()) {
                    log.info("Successfully fetched primary verified email from GitHub /user/emails endpoint.");
                    return primaryVerifiedEmail.get();
                } else {
                    log.warn("Could not find a primary and verified email in the response from GitHub /user/emails.");
                    return null; // Không tìm thấy email phù hợp
                }
            } else {
                log.error("Failed to fetch emails from GitHub /user/emails. Status code: {}", response.getStatusCode());
                return null; // Lỗi khi gọi API
            }
        } catch (Exception e) {
            log.error("Error calling GitHub /user/emails endpoint: {}", e.getMessage(), e);
            return null; // Lỗi ngoại lệ khi gọi API
        }
    }


    // Phương thức createUser và setNames giữ nguyên như trước
    private User createUser(Map<String, Object> attributes, String email, String provider) {
        // ... (giữ nguyên code createUser)
        User user = new User();
        user.setEmail(email);
        user.setActive(true); // New users via OAuth are active by default

        String name = null;
        String imageUrl = null;
        String providerId = null;

        if ("google".equalsIgnoreCase(provider)) {
            name = (String) attributes.get("name");
            imageUrl = (String) attributes.get("picture");
            providerId = (String) attributes.get("sub"); // Google's unique ID
        } else if ("github".equalsIgnoreCase(provider)) {
            name = (String) attributes.get("name");
            if (!StringUtils.hasText(name)) {
                name = (String) attributes.get("login"); // Fallback to login username
            }
            imageUrl = (String) attributes.get("avatar_url");
            // GitHub's ID is typically an integer, convert safely
            Object idObj = attributes.get("id");
            if (idObj != null) {
                providerId = String.valueOf(idObj);
            }
        }

        setNames(user, name);
        user.setImageUrl(imageUrl); // Set image URL if available
        user.setOauthProvider(provider);
        user.setOauthProviderId(providerId); // Store provider-specific ID

        // Generate a secure random password (user won't use it for OAuth login)
        String randomPassword = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));

        // Assign default role
        Role role = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> {
                    log.error("Default role CUSTOMER not found in database!");
                    OAuth2Error error = new OAuth2Error(
                            ROLE_NOT_FOUND_ERROR_CODE,
                            "Default user role (CUSTOMER) could not be found.",
                            null
                    );
                    return new RuntimeException("Default role not found"); // Sẽ được bắt bởi catch chung
                });
        user.setRole(role);

        return userRepository.save(user);
    }

    private void setNames(User user, String fullName) {
        // ... (giữ nguyên code setNames)
        if (StringUtils.hasText(fullName)) {
            String[] names = fullName.trim().split("\\s+"); // Split by any whitespace
            if (names.length > 0) {
                user.setFirstName(names[0]);
                if (names.length > 1) {
                    StringBuilder lastName = new StringBuilder();
                    for (int i = 1; i < names.length; i++) {
                        lastName.append(names[i]).append(" ");
                    }
                    user.setLastName(lastName.toString().trim());
                } else {
                    user.setLastName(""); // Set empty if only one name part
                }
            }
        } else {
            // Provide a sensible default if name is missing
            user.setFirstName("User");
            user.setLastName(String.valueOf(System.currentTimeMillis())); // Or use part of email/ID
        }
    }
}