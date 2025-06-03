package com.webanhang.team_project.service.payment;

import com.webanhang.team_project.enums.PaymentMethod;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.PaymentDetail;
import com.webanhang.team_project.repository.PaymentRepository;
import com.google.gson.Gson;
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
import java.net.URLDecoder;
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

    @Override
    @Transactional
    public String createPayment(Long orderId) {
        try {
            Order order = orderService.findOrderById(orderId);
            if (order == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
            }
            if (order.getTotalDiscountedPrice() == null || order.getTotalDiscountedPrice() <= 0) {
                throw new RuntimeException("Tổng tiền đơn hàng không hợp lệ.");
            }

            order.setPaymentMethod(PaymentMethod.VNPAY);

            String vnp_TxnRef = orderId + "_" + getRandomNumber(8);
            String vnp_OrderInfo = "Thanh toan don hang #" + orderId;
            String vnp_OrderType = "other";
            String vnp_IpAddr = getIpAddress();
            long amount = order.getTotalDiscountedPrice() * 100L;

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
            vnp_Params.put("vnp_ReturnUrl", vnp_Returnurl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Tạo và lưu PaymentDetail
            PaymentDetail paymentDetail = new PaymentDetail();
            paymentDetail.setOrder(order);
            paymentDetail.setPaymentMethod(PaymentMethod.VNPAY);
            paymentDetail.setPaymentStatus(PaymentStatus.PENDING);
            paymentDetail.setTotalAmount(order.getTotalDiscountedPrice());
            paymentDetail.setTransactionId(vnp_TxnRef);
            paymentDetail.setCreatedAt(LocalDateTime.now());

            // FIX: Tạo hash và query URL nhất quán
            String[] hashAndQuery = buildHashAndQuery(vnp_Params);
            String vnp_SecureHash = hashAndQuery[0];
            String queryUrl = hashAndQuery[1] + "&vnp_SecureHash=" + vnp_SecureHash;

            paymentDetail.setVnp_SecureHash(vnp_SecureHash);
            paymentRepository.save(paymentDetail);

            return vnp_PayUrl + "?" + queryUrl;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo yêu cầu thanh toán VNPay: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentDetail getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán với ID: " + paymentId));
    }

    @Override
    @Transactional
    public PaymentDetail processPaymentCallback(Map<String, String> vnpParams) {
        System.out.println("[PaymentServiceImpl] Processing VNPAY Callback with params: " + vnpParams);
        try {
            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
            if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
                throw new IllegalArgumentException("vnp_TxnRef không được để trống trong params callback.");
            }

            PaymentDetail payment = paymentRepository.findByTransactionId(vnp_TxnRef)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với vnp_TxnRef: " + vnp_TxnRef));

            // FIX: Xác minh SecureHash với cách xử lý đúng
            if (!verifySecureHash(vnpParams)) {
                System.err.println("[PaymentServiceImpl] SecureHash verification failed for TxnRef: " + vnp_TxnRef);
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setPaymentLog("SecureHash verification failed. " + new Gson().toJson(vnpParams));
                payment.setVnp_ResponseCode("97");
                return paymentRepository.save(payment);
            }
            System.out.println("[PaymentServiceImpl] SecureHash verified successfully for TxnRef: " + vnp_TxnRef);

            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            if (vnp_ResponseCode == null && vnpParams.containsKey("vnp_TransactionStatus")) {
                vnp_ResponseCode = vnpParams.get("vnp_TransactionStatus");
            }
            payment.setVnp_ResponseCode(vnp_ResponseCode);

            if ("00".equals(vnp_ResponseCode)) {
                System.out.println("[PaymentServiceImpl] Payment successful for TxnRef: " + vnp_TxnRef);
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());

                Order order = payment.getOrder();
                if (order == null) {
                    throw new RuntimeException("Không tìm thấy đơn hàng liên kết với payment TxnRef: " + vnp_TxnRef);
                }
                order.setPaymentStatus(PaymentStatus.COMPLETED);
            } else {
                System.err.println("[PaymentServiceImpl] Payment failed or pending for TxnRef: " + vnp_TxnRef + ", ResponseCode: " + vnp_ResponseCode);
                payment.setPaymentStatus(PaymentStatus.FAILED);
                Order order = payment.getOrder();
                if (order != null) {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                }
            }
            payment.setPaymentLog(new Gson().toJson(vnpParams));
            return paymentRepository.save(payment);

        } catch (Exception e) {
            System.err.println("[PaymentServiceImpl] Exception during processPaymentCallback: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi xử lý callback thanh toán VNPay: " + e.getMessage(), e);
        }
    }

    /**
     * FIX: Phương thức tạo hash và query string nhất quán
     */
    private String[] buildHashAndQuery(Map<String, String> params) {
        try {
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = params.get(fieldName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    // Hash data - không encode
                    hashData.append(fieldName).append("=").append(fieldValue);

                    // Query string - encode UTF-8
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                            .append("=")
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    if (i < fieldNames.size() - 1) {
                        hashData.append("&");
                        query.append("&");
                    }
                }
            }

            String secureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
            return new String[]{secureHash, query.toString()};

        } catch (Exception e) {
            throw new RuntimeException("Error building hash and query: " + e.getMessage(), e);
        }
    }

    /**
     * FIX: Phương thức verify SecureHash đúng cách
     */
    private boolean verifySecureHash(Map<String, String> params) {
        try {
            String receivedHash = params.get("vnp_SecureHash");
            if (receivedHash == null || receivedHash.isEmpty()) {
                return false;
            }

            // Tạo map mới không chứa các tham số hash và step
            Map<String, String> sortedParams = new TreeMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Loại bỏ các tham số không cần thiết cho hash
                if (!key.equals("vnp_SecureHash") &&
                        !key.equals("vnp_SecureHashType") &&
                        !key.equals("step") &&
                        value != null && !value.isEmpty()) {

                    // Decode value nếu cần thiết
                    try {
                        String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8);
                        sortedParams.put(key, decodedValue);
                    } catch (Exception e) {
                        // Nếu decode lỗi, dùng giá trị gốc
                        sortedParams.put(key, value);
                    }
                }
            }

            // Tạo hash data
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (!first) {
                    hashData.append("&");
                }
                hashData.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }

            String calculatedHash = hmacSHA512(vnp_HashSecret, hashData.toString());

            System.out.println("[DEBUG] Hash data: " + hashData.toString());
            System.out.println("[DEBUG] Calculated hash: " + calculatedHash);
            System.out.println("[DEBUG] Received hash: " + receivedHash);

            return calculatedHash.equalsIgnoreCase(receivedHash);

        } catch (Exception e) {
            System.err.println("Error verifying secure hash: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

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
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            return ipAddress;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key or data cannot be null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception ex) {
            System.err.println("Error generating HMAC SHA512: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Cannot generate HMAC SHA512", ex);
        }
    }
}