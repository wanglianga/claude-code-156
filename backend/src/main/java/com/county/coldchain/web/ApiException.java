package com.county.coldchain.web;

import org.springframework.http.HttpStatus;

/** 业务异常：返回 400 与提示信息 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
