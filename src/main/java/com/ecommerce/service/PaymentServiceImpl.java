package com.ecommerce.service;

import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.Order;
import com.ecommerce.model.PaymentDetail;
import com.ecommerce.repository.PaymentRepository;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
public class PaymentServiceImpl implements PaymentService {

    @Value("${vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnp_HashSecret;

    @Value("${vnpay.pay-url}")
    private String vnp_PayUrl;

    @Value("${vnpay.return-url}")
    private String vnp_Returnurl;

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CartService cartService;

    @Override
    @Transactional
    public String createPayment(Long orderId) throws GlobalExceptionHandler {
        try {
            // Lấy thông tin đơn hàng
            Order order = orderService.findOrderById(orderId);
            if (order == null) {
                throw new GlobalExceptionHandler("Không tìm thấy đơn hàng", "ORDER_NOT_FOUND");
            }

            // Tạo mã giao dịch ngẫu nhiên
            String vnp_TxnRef = getRandomNumber(8);
            
            // Thông tin thanh toán
            String vnp_OrderInfo = "Thanh toan don hang #" + orderId;
            String vnp_OrderType = "billpayment";
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
            vnp_Params.put("vnp_ReturnUrl", vnp_Returnurl + "?orderId=" + orderId);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            // Tạo chi tiết thanh toán và lưu vào DB
            PaymentDetail paymentDetail = new PaymentDetail();
            paymentDetail.setOrder(order);
            paymentDetail.setPaymentMethod(PaymentMethod.VNPAY);
            paymentDetail.setPaymentStatus(PaymentStatus.PENDING);
            paymentDetail.setTotalAmount(order.getTotalAmount());
            paymentDetail.setTransactionId(vnp_TxnRef);
            paymentDetail.setCreatedAt(LocalDateTime.now());
            


            // Tạo ngày thanh toán theo múi giờ GMT+7
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            // Thêm thời gian hết hạn (15 phút)
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Thông tin khách hàng
            if (order.getShippingAddress() != null) {
                vnp_Params.put("vnp_Bill_Mobile", order.getShippingAddress().getMobile());
                vnp_Params.put("vnp_Bill_FirstName", order.getShippingAddress().getFirstName());
                vnp_Params.put("vnp_Bill_LastName", order.getShippingAddress().getLastName());
                vnp_Params.put("vnp_Bill_Address", order.getShippingAddress().getStreetAddress());
                vnp_Params.put("vnp_Bill_City", order.getShippingAddress().getCity());
                vnp_Params.put("vnp_Bill_Country", "VN");
            }

            // Sắp xếp tham số và tạo chuỗi hash
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Xây dựng dữ liệu hash
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Xây dựng query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
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

//            cartService.clearCart(order.getUser().getId());

            // URL thanh toán hoàn chỉnh
            return vnp_PayUrl + "?" + queryUrl;
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Lỗi khi tạo yêu cầu thanh toán: " + e.getMessage(), "PAYMENT_ERROR");
        }
    }

    @Override
    public PaymentDetail getPaymentById(Long paymentId) throws GlobalExceptionHandler {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new GlobalExceptionHandler("Không tìm thấy thông tin thanh toán: " + paymentId, "PAYMENT_NOT_FOUND"));
    }
    
    @Override
    @Transactional
    public PaymentDetail processPaymentCallback(Map<String, String> vnpParams) throws GlobalExceptionHandler {
        try {
            // Lấy các tham số quan trọng
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
            String vnp_Amount = vnpParams.get("vnp_Amount");
            String vnp_OrderInfo = vnpParams.get("vnp_OrderInfo");
            String vnp_TransactionNo = vnpParams.get("vnp_TransactionNo");
            String vnp_BankCode = vnpParams.get("vnp_BankCode");
            String vnp_PayDate = vnpParams.get("vnp_PayDate");
            
            // Xác minh chữ ký
            String secureHash = vnpParams.get("vnp_SecureHash");
            
            // Tìm PaymentDetail dựa trên vnp_TxnRef
            PaymentDetail payment = paymentRepository.findByTransactionId(vnp_TxnRef)
                    .orElseThrow(() -> new GlobalExceptionHandler("Không tìm thấy giao dịch: " + vnp_TxnRef, "TRANSACTION_NOT_FOUND"));
            
            // Kiểm tra mã phản hồi
            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công


                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setPaymentLog(new Gson().toJson(vnpParams));
                payment.setVnp_ResponseCode(vnp_ResponseCode);
                
                // Cập nhật trạng thái thanh toán cho đơn hàng
                Order order = payment.getOrder();
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                
                // Lưu thông tin thanh toán
                return paymentRepository.save(payment);
            } else {
                // Thanh toán thất bại
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setPaymentLog(new Gson().toJson(vnpParams));
                return paymentRepository.save(payment);
            }
        } catch (Exception e) {
            throw new GlobalExceptionHandler("Lỗi xử lý callback thanh toán: " + e.getMessage(), "PAYMENT_CALLBACK_ERROR");
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