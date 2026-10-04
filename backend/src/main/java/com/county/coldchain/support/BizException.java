package com.county.coldchain.support;

/** 业务异常：由全局处理器转为 400 响应。 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
