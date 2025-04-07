package com.ecommerce.service;

import com.ecommerce.exception.GlobalExceptionHandler;
import com.ecommerce.model.PaymentDetail;

import java.util.Map;

public interface PaymentService {
    String createPayment(Long orderId) throws GlobalExceptionHandler;
    PaymentDetail getPaymentById(Long paymentId) throws GlobalExceptionHandler;
    PaymentDetail processPaymentCallback(Map<String, String> vnpParams) throws GlobalExceptionHandler;
} 