package com.haot.coupon.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T> (
        String statusCode,
        String status,
        String message,
        List<String> errorList,
        T data
) {
    private static final String SUCCESS = "SUCCESS";
    private static final String ERROR = "ERROR";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("4000", SUCCESS, "API 요청에 성공했습니다", data);
    }

    public static <T> ApiResponse<T> success(ResCodeIfs resCodeIfs, T data) {
        return new ApiResponse<>(resCodeIfs.getCode(), SUCCESS, resCodeIfs.getMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("4000", SUCCESS, "API 요청에 성공했습니다",null);
    }

    public static ApiResponse<Void> success(ResCodeIfs resCodeIfs) {
        return new ApiResponse<>(resCodeIfs.getCode(), SUCCESS, resCodeIfs.getMessage(),null);
    }

    public static ApiResponse<Object> error(ResCodeIfs resCodeIfs){
        return new ApiResponse<>(resCodeIfs.getCode(), ERROR, resCodeIfs.getMessage(), null, null);
    }

    public static ApiResponse<Object> error(ResCodeIfs resCodeIfs, List<String> errorList) {
        return new ApiResponse<>(resCodeIfs.getCode(), ERROR, resCodeIfs.getMessage(), errorList);
    }

    public ApiResponse(String statusCode, String status, String message, T data){
        this(statusCode, status, message, null, data);
    }

    public ApiResponse(String statusCode, String status, String message, List<String> errorList){
        this(statusCode, status, message, errorList, null);
    }

}
