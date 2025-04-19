package com.webanhang.team_project.service.payment;

import com.webanhang.team_project.enums.PaymentMethod;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.PaymentDetail;
import com.webanhang.team_project.repository.PaymentRepository;
import com.google.gson.Gson;
import com.webanhang.team_project.service.cart.ICartService;
import com.webanhang.team_project.service.order.IOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnp_HashSecret;

    @Value("${vnpay.pay-url}")
    private String vnp_PayUrl;

    @Value("${vnpay.return-url}")
    private String vnp_Returnurl;

    private final IOrderService orderService;

    private final PaymentRepository paymentRepository;

    private final ICartService ICartService;

    @Override
    @Transactional
    public String createPayment(Long orderId) {
        try {
            // Lấy thông tin đơn hàng
            Order order = orderService.findOrderById(orderId);
            if (order == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng");
            }

            order.setPaymentMethod(PaymentMethod.VNPAY);
            // Tạo mã giao dịch ngẫu nhiên - đảm bảo duy nhất
            String vnp_TxnRef = orderId + "_" + getRandomNumber(8);

            // Thông tin thanh toán
            String vnp_OrderInfo = "Thanh toan don hang #" + orderId;
            String vnp_OrderType = "other"; // Thay đổi từ "billpayment" sang "other"
            String vnp_IpAddr = getIpAddress();
            int amount = order.getTotalAmount() * 100; // Chuyển sang xu (VND x 100)

            // Tạo map các tham số
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", "vn");

            // Sửa Return URL - không đính kèm orderId vào URL
            vnp_Params.put("vnp_ReturnUrl", vnp_Returnurl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            // Tạo ngày thanh toán theo múi giờ GMT+7
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            // Thêm thời gian hết hạn (15 phút)
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Tạo chi tiết thanh toán và lưu vào DB
            PaymentDetail paymentDetail = new PaymentDetail();
            paymentDetail.setOrder(order);
            paymentDetail.setPaymentMethod(PaymentMethod.VNPAY);
            paymentDetail.setPaymentStatus(PaymentStatus.PENDING);
            paymentDetail.setTotalAmount(order.getTotalAmount());
            paymentDetail.setTransactionId(vnp_TxnRef);
            paymentDetail.setCreatedAt(LocalDateTime.now());

            // Sắp xếp tham số và tạo chuỗi hash
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Xây dựng dữ liệu hash
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    // Xây dựng query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)); // SỬA THÀNH UTF_8
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            // Tạo secure hash
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

            paymentDetail.setVnp_SecureHash(vnp_SecureHash);
            // Lưu chi tiết thanh toán
            paymentRepository.save(paymentDetail);

            // URL thanh toán hoàn chỉnh
            return vnp_PayUrl + "?" + queryUrl;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo yêu cầu thanh toán: " + e.getMessage());
        }
    }

    @Override
    public PaymentDetail getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán: " + paymentId));
    }

    @Override
    @Transactional
    public PaymentDetail processPaymentCallback(Map<String, String> vnpParams) {
        try {
            // Log để debug
            System.out.println("Processing payment callback with params: " + vnpParams);

            // Lấy các tham số quan trọng
            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");

            // Lấy mã phản hồi, sử dụng vnp_ResponseCode hoặc vnp_TransactionStatus
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            if (vnp_ResponseCode == null || vnp_ResponseCode.isEmpty()) {
                vnp_ResponseCode = vnpParams.get("vnp_TransactionStatus");
            }

            if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
                throw new RuntimeException("Mã giao dịch không hợp lệ hoặc không được cung cấp");
            }

            // Tìm PaymentDetail dựa trên vnp_TxnRef
            PaymentDetail payment = paymentRepository.findByTransactionId(vnp_TxnRef)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + vnp_TxnRef));

            // Kiểm tra trạng thái thanh toán hiện tại để tránh xử lý trùng lặp
            if (PaymentStatus.COMPLETED.equals(payment.getPaymentStatus())) {
                return payment; // Đã xử lý rồi, trả về thông tin hiện tại
            }

            // Xác minh chữ ký
            Map<String, String> vnp_Params = new HashMap<>(vnpParams);

            // Loại bỏ vnp_SecureHash ra khỏi danh sách tham số để tính lại hash
            String vnp_SecureHash = vnp_Params.remove("vnp_SecureHash");
            String vnp_SecureHashType = vnp_Params.remove("vnp_SecureHashType");

            // Kiểm tra chữ ký
            if (vnp_SecureHash != null) {
                // Sắp xếp tham số theo thứ tự bảng chữ cái
                List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
                Collections.sort(fieldNames);
                StringBuilder hashData = new StringBuilder();

                for (String fieldName : fieldNames) {
                    String fieldValue = vnp_Params.get(fieldName);
                    if ((fieldValue != null) && (fieldValue.length() > 0)) {
                        hashData.append(fieldName);
                        hashData.append('=');
                        hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                        if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                            hashData.append('&');
                        }
                    }
                }

                // Tính toán lại secure hash
                String calculatedHash = hmacSHA512(vnp_HashSecret, hashData.toString());

                // So sánh hash tính toán với hash nhận được
                if (!calculatedHash.equals(vnp_SecureHash)) {
                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    payment.setPaymentLog("Invalid hash signature");
                    return paymentRepository.save(payment);
                }
            }

            // Lưu thông tin response vào log
            payment.setPaymentLog(new Gson().toJson(vnpParams));
            payment.setVnp_ResponseCode(vnp_ResponseCode);

            // Kiểm tra mã phản hồi
            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());

                // Cập nhật trạng thái thanh toán cho đơn hàng
                Order order = payment.getOrder();
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                // Nếu cần cập nhật các trạng thái khác của đơn hàng, thêm vào đây

            } else {
                // Thanh toán thất bại
                payment.setPaymentStatus(PaymentStatus.FAILED);
            }

            // Lưu thông tin thanh toán
            return paymentRepository.save(payment);
        } catch (Exception e) {
            // Log chi tiết lỗi
            e.printStackTrace();
            throw new RuntimeException("Lỗi xử lý callback thanh toán: " + e.getMessage());
        }
    }

    // Các phương thức hỗ trợ
    private String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}