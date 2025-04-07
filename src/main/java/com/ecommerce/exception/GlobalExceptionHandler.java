package com.ecommerce.exception;

import com.ecommerce.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {
    private String code;

    public GlobalExceptionHandler() {
        super();
        this.code = "GLOBAL_ERROR";
    }

    public GlobalExceptionHandler(String message) {
        super(message);
        this.code = "GLOBAL_ERROR";
    }

    public GlobalExceptionHandler(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @ExceptionHandler(GlobalExceptionHandler.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(GlobalExceptionHandler e) {
        ErrorResponse error = new ErrorResponse(e.getMessage(), e.getCode());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = new ErrorResponse(e.getMessage(), "INTERNAL_SERVER_ERROR");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}