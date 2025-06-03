package com.webanhang.team_project.service.payment;

import com.webanhang.team_project.enums.PaymentMethod;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.PaymentDetail;
import com.webanhang.team_project.repository.PaymentRepository;
import com.google.gson.Gson;
// import com.webanhang.team_project.service.cart.ICartService; // Bỏ nếu không dùng
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
    private String vnp_Returnurl; // Đảm bảo giá trị này được cấu hình đúng trong application.properties

    private final IOrderService orderService;
    private final PaymentRepository paymentRepository;
    // private final ICartService ICartService; // Bỏ nếu không dùng trực tiếp ở đây

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

            // QUAN TRỌNG: Gán PaymentMethod cho Order trước khi lưu PaymentDetail
            order.setPaymentMethod(PaymentMethod.VNPAY);
            // Cân nhắc cập nhật trạng thái đơn hàng ở đây nếu cần, ví dụ: PENDING_PAYMENT
            // orderService.saveOrder(order); // Lưu thay đổi paymentMethod vào Order nếu cần thiết

            String vnp_TxnRef = orderId + "_" + getRandomNumber(8); // Mã giao dịch tham chiếu
            String vnp_OrderInfo = "Thanh toan don hang #" + orderId;
            String vnp_OrderType = "other";
            String vnp_IpAddr = getIpAddress();
            long amount = order.getTotalDiscountedPrice() * 100L; // VNPay yêu cầu nhân 100

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
            vnp_Params.put("vnp_ReturnUrl", vnp_Returnurl); // URL VNPay sẽ redirect về sau khi thanh toán
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn thanh toán
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Tạo và lưu PaymentDetail
            PaymentDetail paymentDetail = new PaymentDetail();
            paymentDetail.setOrder(order);
            paymentDetail.setPaymentMethod(PaymentMethod.VNPAY); // Gán PaymentMethod
            paymentDetail.setPaymentStatus(PaymentStatus.PENDING);
            paymentDetail.setTotalAmount(order.getTotalDiscountedPrice());
            paymentDetail.setTransactionId(vnp_TxnRef); // Lưu vnp_TxnRef để đối chiếu khi callback
            paymentDetail.setCreatedAt(LocalDateTime.now());
            // paymentDetail = paymentRepository.save(paymentDetail); // Lưu trước khi tạo hash để có ID nếu cần thiết

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII)); // VNPay thường dùng US_ASCII cho hash

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

            paymentDetail.setVnp_SecureHash(vnp_SecureHash); // Lưu hash để kiểm tra lại nếu cần
            paymentRepository.save(paymentDetail); // Lưu paymentDetail sau khi có hash

            return vnp_PayUrl + "?" + queryUrl;
        } catch (Exception e) {
            // Log lỗi chi tiết ở backend
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
        System.out.println("[PaymentServiceImpl] Processing VNPAY Callback with params: " + vnpParams); // LOGGING
        try {
            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
            if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
                throw new IllegalArgumentException("vnp_TxnRef không được để trống trong params callback.");
            }

            PaymentDetail payment = paymentRepository.findByTransactionId(vnp_TxnRef)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với vnp_TxnRef: " + vnp_TxnRef));

            // Xác minh SecureHash (Rất quan trọng)
            // Tạo lại hashData từ params nhận được (trừ vnp_SecureHash)
            Map<String, String> paramsToHashMap = new HashMap<>(vnpParams);
            paramsToHashMap.remove("vnp_SecureHashType"); // Bỏ nếu có
            paramsToHashMap.remove("vnp_SecureHash");     // Bỏ hash gốc

            List<String> fieldNames = new ArrayList<>(paramsToHashMap.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = paramsToHashMap.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    // Quan trọng: VNPay dùng URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()) cho dữ liệu hash
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            String calculatedSecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
            String receivedSecureHash = vnpParams.get("vnp_SecureHash");

            if (!calculatedSecureHash.equalsIgnoreCase(receivedSecureHash)) {
                System.err.println("[PaymentServiceImpl] SecureHash mismatch! Calculated: " + calculatedSecureHash + ", Received: " + receivedSecureHash);
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setPaymentLog("SecureHash mismatch. " + new Gson().toJson(vnpParams));
                payment.setVnp_ResponseCode("97"); // Mã lỗi cho sai chữ ký
                // Không cập nhật trạng thái đơn hàng nếu hash sai
                return paymentRepository.save(payment);
            }
            System.out.println("[PaymentServiceImpl] SecureHash verified successfully for TxnRef: " + vnp_TxnRef);


            // Tiếp tục xử lý nếu hash đúng
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            if (vnp_ResponseCode == null && vnpParams.containsKey("vnp_TransactionStatus")) {
                vnp_ResponseCode = vnpParams.get("vnp_TransactionStatus");
            }
            payment.setVnp_ResponseCode(vnp_ResponseCode); // Lưu response code

            if ("00".equals(vnp_ResponseCode)) {
                System.out.println("[PaymentServiceImpl] Payment successful for TxnRef: " + vnp_TxnRef);
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now()); // Thời gian hoàn tất thanh toán

                Order order = payment.getOrder();
                if (order == null) {
                    throw new RuntimeException("Không tìm thấy đơn hàng liên kết với payment TxnRef: " + vnp_TxnRef);
                }
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                // orderService.saveOrder(order); // Lưu lại order đã cập nhật paymentStatus
            } else {
                System.err.println("[PaymentServiceImpl] Payment failed or pending for TxnRef: " + vnp_TxnRef + ", ResponseCode: " + vnp_ResponseCode);
                payment.setPaymentStatus(PaymentStatus.FAILED);
                Order order = payment.getOrder();
                if (order != null) {
                    order.setPaymentStatus(PaymentStatus.FAILED); // Cập nhật trạng thái đơn hàng thất bại
                    // orderService.saveOrder(order);
                }
            }
            payment.setPaymentLog(new Gson().toJson(vnpParams)); // Lưu toàn bộ log params
            return paymentRepository.save(payment);

        } catch (Exception e) {
            System.err.println("[PaymentServiceImpl] Exception during processPaymentCallback for params: " + vnpParams + " - Error: " + e.getMessage());
            e.printStackTrace();
            // Cân nhắc: không nên ném RuntimeException chung chung ở đây nếu có thể.
            // Nếu tìm thấy payment và có lỗi xử lý, có thể cập nhật trạng thái payment là FAILED.
            // Nếu không tìm thấy payment, thì đó là lỗi nghiêm trọng.
            throw new RuntimeException("Lỗi khi xử lý callback thanh toán VNPay: " + e.getMessage(), e);
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
            // Fallback IP nếu không lấy được từ request (ví dụ: chạy trong môi trường test không có request)
            return "127.0.0.1";
        }
    }

    private String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8); // Quan trọng: dùng UTF-8
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8); // Quan trọng: dùng UTF-8
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception ex) {
            // Log lỗi chi tiết hơn
            System.err.println("Error generating HMAC SHA512: " + ex.getMessage());
            ex.printStackTrace();
            return ""; // Hoặc ném một exception cụ thể
        }
    }
}