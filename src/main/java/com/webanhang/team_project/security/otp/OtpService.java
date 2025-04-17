package com.webanhang.team_project.security.otp;

import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
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

    @Value("${app.otp.resend-cooldown-minutes}")
    private int resendCooldownMinutes;

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
                LocalDateTime.now().plusMinutes(otpExpirationMinutes),
                LocalDateTime.now() // Lưu thời gian tạo OTP
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
        } else if (LocalDateTime.now().isAfter(otpData.getExpirationTime())) {
            // Nếu OTP hết hạn, xóa nó khỏi storage
            otpStorage.remove(email);
        }
        return isValid;
    }

    /// --- New: Check if resend is allowed based on cooldown ---
    public boolean isResendAllowed(String email) {
        OtpData existingOtpData = otpStorage.get(email);
        if (existingOtpData == null) {
            return true; // No previous OTP sent recently, so allowed
        }
        // Calculate the earliest time a resend is allowed
        LocalDateTime allowedResendTime = existingOtpData.getGenerationTime()
                .plusMinutes(resendCooldownMinutes);

        // Check if the current time is after the allowed resend time
        return LocalDateTime.now().isAfter(allowedResendTime);
    }

    /// --- New Helper: Get remaining cooldown seconds ---
    public long getRemainingCooldownSeconds(String email) {
        OtpData existingOtpData = otpStorage.get(email);
        if (existingOtpData == null) {
            return 0;
        }
        LocalDateTime lastSentTime = existingOtpData.getGenerationTime();
        LocalDateTime allowedResendTime = lastSentTime.plusMinutes(resendCooldownMinutes);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(allowedResendTime)) {
            // Calculate remaining duration and return seconds
            return Duration.between(now, allowedResendTime).getSeconds();
        }
        return 0; // Cooldown has passed
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


    @Getter
    @AllArgsConstructor// Use Lombok Getter for cleaner code
    private static class OtpData {
        private final String otp;
        private final LocalDateTime expirationTime;
        private final LocalDateTime generationTime; // Added generation time
    }
}