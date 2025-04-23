package com.webanhang.team_project.security.otp;

import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException; // Import
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Value("${app.otp.resend-cooldown-minutes}")
    private int resendCooldownMinutes;

    @Value("${app.company.logo.url}") // Thêm URL logo từ config
    private String companyLogoUrl;

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

        // Check if OTP matches and is not expired
        boolean isValid = otpData.getOtp().equals(otp) &&
                LocalDateTime.now().isBefore(otpData.getExpirationTime());

        if (isValid) {
            // Nếu OTP hợp lệ, kích hoạt tài khoản (nếu chưa active)
            // It's generally better to activate *only* during registration verification,
            // not during password reset OTP validation. Let's refine activateUserAccount.
            User user = userRepository.findByEmail(email);

            if (user.isActive()==true) {
                throw new RuntimeException("Tài khoản đã được kích hoạt trước đó");
            }

            if (user != null && !user.isActive()) {
                activateUserAccount(email); // Activate only if not already active
            }
            // Xóa OTP sau khi đã dùng thành công
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
     * Gửi email chứa OTP với định dạng HTML
     */
    public void sendOtpEmail(String email, String otp) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            // true = multipart message
            // true = enable HTML content
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = getHtmlEmailTemplate(otp, otpExpirationMinutes, companyLogoUrl);

            helper.setTo(email);
            helper.setSubject("Mã xác thực tài khoản của bạn"); // Chủ đề email
            helper.setText(htmlContent, true); // true để chỉ định đây là nội dung HTML

            mailSender.send(mimeMessage);
            log.info("Đã gửi email OTP HTML tới {}", email);

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email OTP HTML tới {}: {}", email, e.getMessage());
            // Xử lý lỗi phù hợp (ví dụ: throw exception tùy chỉnh)
            throw new RuntimeException("Không thể gửi email OTP.", e);
        }
    }

    /**
     * Helper method để tạo nội dung HTML từ template
     */
    private String getHtmlEmailTemplate(String otpCode, int expirationMinutes, String logoUrl) {
        // Bạn có thể đọc template từ file thay vì hardcode chuỗi ở đây
        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mã Xác Minh Email</title>
            </head>
            <body>
                <div style="max-width: 600px; margin: 20px auto; padding: 20px; background-color: #ffffff; border: 1px solid #dddddd; font-family: Arial, Helvetica, sans-serif; color: #333333; line-height: 1.6;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <img src="%s" alt="Logo Công Ty" style="max-width: 100px; display: block; margin: 0 auto;">
                    </div>
                    <div style="border-top: 2px solid #20C9D9; margin: 20px 0;"></div>
                    <h2 style="text-align: center; color: #333333; font-size: 18px; margin-bottom: 15px;">Mã xác minh email, thời gian hiệu lực: %d phút</h2>
                    <p style="margin: 10px 0; color: #333333; font-size: 14px; text-align: center;">Đây là mã xác minh email của bạn:</p>
                    <div style="font-size: 42px; font-weight: bold; color: #20C9D9; text-align: center; margin: 30px 0; letter-spacing: 2px;">
                        %s
                    </div>
                    <p style="font-size: 12px; color: #888888; margin-top: 15px; text-align: center;">
                        *Lưu ý: không được chuyển tiếp email hoặc mã này đến bất kỳ tài khoản Email không liên quan nào khác.<br>Mã này chỉ có hiệu lực trong %d phút.
                    </p>
                    
                    <p style="font-size: 10px; color: #888888; margin-top: 15px; text-align: center; border-top: 2px solid #000000">
                                Bạn có câu hỏi hay vấn đề gì cần trợ giúp? <br>
                                Bạn có thể gửi email đến techshopprojectteam@gmail.com Để liên hệ với chúng tôi. <br>
                                Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi! <br>
                                *Email này được gửi tự động và bạn không cần trả lời lại. 
                    </p>
                </div>
            </body>
            </html>
        """;
        // Sử dụng String.format để thay thế các placeholder (%s cho chuỗi, %d cho số nguyên)
        return String.format(template, logoUrl, expirationMinutes, otpCode, expirationMinutes);
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