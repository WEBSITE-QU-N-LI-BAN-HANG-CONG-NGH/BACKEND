package com.webanhang.team_project.security.otp;

import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    // Lưu trữ OTP và thời gian hết hạn (email -> [otp, expirationTime])
    private final Map<String, OtpData> otpStorage = new HashMap<>();

    /**
     * Tạo OTP ngẫu nhiên 6 số
     */
    public String generateOtp(String email) {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // OTP 6 số
        String otpString = String.valueOf(otp);

        // Lưu OTP và thời gian hết hạn
        otpStorage.put(email, new OtpData(
                otpString,
                LocalDateTime.now().plusMinutes(otpExpirationMinutes)
        ));

        return otpString;
    }

    /**
     * Kiểm tra OTP có hợp lệ không
     */
    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);

        if (otpData == null) {
            return false;
        }

        boolean isValid = otpData.getOtp().equals(otp) &&
                LocalDateTime.now().isBefore(otpData.getExpirationTime());

        if (isValid) {
            // Nếu OTP hợp lệ, kích hoạt tài khoản
            activateUserAccount(email);
            // Xóa OTP sau khi đã dùng
            otpStorage.remove(email);
        }

        return isValid;
    }

    /**
     * Gửi email chứa OTP
     */
    public void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã xác thực đăng ký tài khoản");
        message.setText("Mã xác thực của bạn là: " + otp + "\nMã này sẽ hết hạn sau " +
                otpExpirationMinutes + " phút.");

        mailSender.send(message);
    }

    /**
     * Kích hoạt tài khoản user sau khi xác thực OTP thành công
     */
    private void activateUserAccount(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setActive(true);
            userRepository.save(user);
        }
    }

    private static class OtpData {
        private final String otp;
        private final LocalDateTime expirationTime;

        public OtpData(String otp, LocalDateTime expirationTime) {
            this.otp = otp;
            this.expirationTime = expirationTime;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpirationTime() {
            return expirationTime;
        }
    }
}