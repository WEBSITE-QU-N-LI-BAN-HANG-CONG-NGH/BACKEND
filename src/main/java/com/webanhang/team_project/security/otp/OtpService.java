package com.webanhang.team_project.security.otp;

import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.OrderItem;
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
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException; // Import

import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;
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
    private final TemplateEngine templateEngine;

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

            // Chuẩn bị context cho Thymeleaf
            Context context = new Context();
            context.setVariable("otpCode", otp);
            context.setVariable("otpExpirationMinutes", otpExpirationMinutes);
            context.setVariable("companyLogoUrl", companyLogoUrl);
            // context.setVariable("userEmail", email); // Có thể thêm nếu cần hiển thị email trong template

            // Xử lý template
            String htmlContent = templateEngine.process("mail/otp-verification-email", context);

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

    public void sendOrderMail(String email, Order order) {
        if (order == null) {
            log.error("Cannot send email for null order");
            throw new RuntimeException("Order not found for sending email.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Chuẩn bị context cho Thymeleaf
            Context context = new Context();
            context.setVariable("order", order); // Truyền đối tượng Order
            context.setVariable("companyLogoUrl", companyLogoUrl); // Truyền URL logo
            // Bạn có thể thêm các biến khác nếu cần trong template

            // Xử lý template
            String htmlContent = templateEngine.process("mail/order-confirmation-email", context); // Đường dẫn đến file template

            helper.setTo(email);
            helper.setSubject("Xác nhận đơn hàng TechShop #" + order.getId());
            helper.setText(htmlContent, true); // true để chỉ định đây là nội dung HTML

            mailSender.send(mimeMessage);
            log.info("Successfully sent order confirmation email to {} for order #{}", email, order.getId());

        } catch (MessagingException e) {
            log.error("MessagingException while sending order confirmation email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email thông báo đơn hàng do lỗi gửi thư.", e);
        } catch (Exception e) {
            log.error("Unexpected exception while sending order confirmation email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Có lỗi không mong muốn xảy ra khi gửi email thông báo đơn hàng.", e);
        }
    }

}