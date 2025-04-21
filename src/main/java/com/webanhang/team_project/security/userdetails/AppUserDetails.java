package com.webanhang.team_project.security.userdetails;

import com.webanhang.team_project.model.User;
import lombok.Getter;
// Bỏ @Setter đi nếu bạn muốn các trường là final và bất biến sau khi tạo
// import lombok.Setter;
// import lombok.NoArgsConstructor; // Không cần thiết nếu bạn định nghĩa constructor rõ ràng
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User; // ** Thêm import này **

import java.util.Collection;
import java.util.List;
import java.util.Map; // ** Thêm import này **

@Getter
// @Setter // Cân nhắc bỏ nếu không thực sự cần thay đổi sau khi tạo
public class AppUserDetails implements UserDetails, OAuth2User { // ** Implement thêm OAuth2User **

    private final Long id; // Nên là final
    private final String email; // Nên là final
    private final String password; // Nên là final
    private final boolean enabled; // Thêm trường này để lưu trạng thái active/enabled
    private final Collection<? extends GrantedAuthority> authorities; // Nên là final

    // ** Thêm trường để lưu các thuộc tính OAuth2 **
    private Map<String, Object> attributes; // Không nên là final nếu cần set sau

    // --- Constructor ---
    // Constructor chính, nhận tất cả các giá trị cần thiết
    public AppUserDetails(Long id, String email, String password, boolean enabled, Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
        this.attributes = attributes;
    }

    // --- Factory Methods ---
    // Factory method cho đăng nhập thông thường (từ User entity)
    public static AppUserDetails buildUserDetails(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().getName().name()));
        // Truyền trạng thái active của User vào constructor
        return new AppUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.isActive(), // Sử dụng trạng thái active từ User
                authorities,
                null // Không có attributes OAuth2 trong trường hợp này
        );
    }

    // Factory method cho đăng nhập OAuth2 (từ User entity và attributes)
    public static AppUserDetails buildOAuth2UserDetails(User user, Map<String, Object> attributes) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().getName().name()));
        // Truyền trạng thái active và attributes vào constructor
        return new AppUserDetails(
                user.getId(),
                user.getEmail(),
                // Mật khẩu có thể không cần thiết hoặc là một giá trị ngẫu nhiên cho OAuth2 user
                user.getPassword(),
                user.isActive(), // Sử dụng trạng thái active từ User
                authorities,
                attributes // Truyền attributes OAuth2
        );
    }


    // --- Methods from UserDetails ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Dùng email làm username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Hoặc return this.enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Hoặc return this.enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Hoặc return this.enabled;
    }

    @Override
    public boolean isEnabled() {
        // Trả về trạng thái active/enabled đã lưu
        return this.enabled;
    }

    // --- Methods from OAuth2User ---
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // Cần setter nếu attributes không được truyền qua constructor và không phải final
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        // Trả về định danh duy nhất trong ngữ cảnh OAuth2
        // Dùng id người dùng nội bộ là một lựa chọn tốt
        return String.valueOf(this.id);
    }
}