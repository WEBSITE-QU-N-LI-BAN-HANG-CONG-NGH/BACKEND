package com.ecommerce.service;

import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    
    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.debug("Bắt đầu loadUser với userRequest: {}", userRequest);
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            logger.debug("OAuth2User loaded với attributes: {}", oAuth2User.getAttributes());
            return processOAuth2User(oAuth2User, userRequest);
        } catch (Exception ex) {
            logger.error("Lỗi trong quá trình loadUser: {}", ex.getMessage(), ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }
    
    private OAuth2User processOAuth2User(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        logger.debug("Xử lý user từ provider: {}", provider);
        
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        logger.debug("Attributes từ OAuth2User: {}", attributes);
        
        String email = null;
        String name = null;
        String picture = null;
        String providerId = null;

        if ("github".equals(provider)) {
            // Thử lấy email từ thông tin cơ bản
            email = (String) attributes.get("email");
            logger.debug("Email từ thông tin cơ bản GitHub: {}", email);
            
            // Nếu không có email, thử lấy từ API email
            if (email == null || email.isEmpty()) {
                email = fetchGitHubEmail(userRequest);
                logger.debug("Email sau khi gọi API bổ sung: {}", email);
                if (email != null) {
                    // Thêm email vào attributes để nó có sẵn trong OAuth2User
                    attributes.put("email", email);
                }
            }
            
            name = (String) attributes.get("name");
            if (name == null || name.isEmpty()) {
                name = (String) attributes.get("login");
                logger.debug("Sử dụng login name từ GitHub: {}", name);
            }
            
            picture = (String) attributes.get("avatar_url");
            providerId = attributes.get("id").toString();
        } else if ("google".equals(provider)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            picture = (String) attributes.get("picture");
            providerId = (String) attributes.get("sub");
        }
        
        if (email == null || email.isEmpty()) {
            logger.error("Không thể lấy email từ provider: {}, attributes: {}", provider, attributes);
            throw new OAuth2AuthenticationException("Email không được tìm thấy từ " + provider);
        }
        
        logger.debug("Đã lấy được thông tin: email={}, name={}, providerId={}", email, name, providerId);
        
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            logger.info("Tạo user mới với email: {}", email);
            user = createUserFromOAuth2(email, name, picture, providerId, provider);
            user = userRepository.save(user);
            logger.info("Đã tạo user mới với ID: {}", user.getId());
        } else {
            logger.info("Cập nhật thông tin cho user hiện có: {}", email);
            updateExistingUser(user, provider, providerId, picture);
        }
        
        // Tạo OAuth2User mới với attributes đã cập nhật
        return new DefaultOAuth2User(
            oAuth2User.getAuthorities(),
            attributes,
            userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName()
        );
    }
    
    private String fetchGitHubEmail(OAuth2UserRequest userRequest) {
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> emails = response.getBody();
            logger.debug("Kết quả API email GitHub: {}", emails);
            
            if (emails != null && !emails.isEmpty()) {
                // Tìm email chính và đã xác minh
                Optional<Map<String, Object>> primaryEmail = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")) && Boolean.TRUE.equals(email.get("verified")))
                    .findFirst();
                
                if (primaryEmail.isPresent()) {
                    String email = (String) primaryEmail.get().get("email");
                    logger.debug("Đã tìm thấy email chính đã xác minh: {}", email);
                    return email;
                }
                
                // Nếu không có email chính, lấy email đã xác minh đầu tiên
                Optional<Map<String, Object>> verifiedEmail = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .findFirst();
                
                if (verifiedEmail.isPresent()) {
                    String email = (String) verifiedEmail.get().get("email");
                    logger.debug("Sử dụng email đã xác minh đầu tiên: {}", email);
                    return email;
                }
            }
            
            logger.warn("Không tìm thấy email phù hợp từ GitHub API");
            return null;
            
        } catch (Exception e) {
            logger.error("Lỗi khi lấy email từ GitHub: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private void updateExistingUser(User user, String provider, String providerId, String picture) {
        boolean updated = false;
        
        if (user.getOauthProvider() == null || provider.equals(user.getOauthProvider())) {
            user.setOauthProvider(provider);
            user.setOauthProviderId(providerId);
            updated = true;
        }
        
        if (picture != null && !picture.equals(user.getImageUrl())) {
            user.setImageUrl(picture);
            updated = true;
        }
        
        if (updated) {
            userRepository.save(user);
            logger.info("Đã cập nhật thông tin OAuth2 cho user: {}", user.getEmail());
        }
    }
    
    private User createUserFromOAuth2(String email, String name, String picture, String providerId, String provider) {
        User user = new User();
        user.setEmail(email);
        
        if (name != null && !name.isEmpty()) {
            String[] nameParts = name.split("\\s+");
            if (nameParts.length > 1) {
                user.setFirstName(nameParts[0]);
                user.setLastName(name.substring(nameParts[0].length()).trim());
            } else {
                user.setFirstName(name);
                user.setLastName("");
            }
        } else {
            String username = email.split("@")[0];
            if ("github".equals(provider)) {
                user.setFirstName("GitHub");
                user.setLastName(username);
            } else {
                user.setFirstName("User");
                user.setLastName(username);
            }
        }
        
        String randomPassword = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setRole("ROLE_CUSTOMER");
        user.setOauthProvider(provider);
        user.setOauthProviderId(providerId);
        
        if (picture != null && !picture.isEmpty()) {
            user.setImageUrl(picture);
        }
        
        return user;
    }
} 