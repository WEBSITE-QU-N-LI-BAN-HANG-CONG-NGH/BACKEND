package com.webanhang.team_project.enums;

public enum OrderStatus {
    PENDING, // chờ xác nhận hoặc thanh toán
    PROCESSING, // đang được xử lý
    SHIPPED, // Đơn hàng đã được gửi đi
    DELIVERED, // Đơn hàng đã được giao thành công
    CANCELLED // Đơn hàng đã bị hủy
}
