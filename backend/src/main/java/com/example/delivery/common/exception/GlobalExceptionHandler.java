package com.example.delivery.common.exception;

import com.example.delivery.common.api.ApiResponse;
import com.example.delivery.common.api.ErrorCode;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.error(exception.getErrorCode().code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        return ApiResponse.error(ErrorCode.PARAM_ERROR.code(), ErrorCode.PARAM_ERROR.message());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message());
    }
}
