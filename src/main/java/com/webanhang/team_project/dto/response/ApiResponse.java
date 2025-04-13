package com.webanhang.team_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private T data;              // Dữ liệu trả về
    private String message;      // Thông báo mô tả
    private Map<String, Object> pagination;  // Thông tin phân trang (nếu có)
    private boolean status = true;  // Trạng thái của response, mặc định là true

    // Constructor cơ bản cho phản hồi thành công
    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
        this.status = true;
    }

    // Constructor cho phản hồi thành công có phân trang
    public ApiResponse(T data, String message, Map<String, Object> pagination) {
        this.data = data;
        this.message = message;
        this.pagination = pagination;
        this.status = true;
    }

    // Constructor cho phản hồi lỗi
    public ApiResponse(String message) {
        this.message = message;
        this.data = null;
    }
    public ApiResponse(String message, boolean status) {
        this.message = message;
        this.data = null;
        this.status = status;
    }


    // Factory methods
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> pagination) {
        return new ApiResponse<>(data, message, pagination);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message);
    }
    public static <T> ApiResponse<T> error(String message, boolean status) {
        return new ApiResponse<>(message, false);
    }
}
