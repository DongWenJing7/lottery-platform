package com.lottery.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException unauthorized() {
        return new BusinessException(401, "请先登录");
    }

    public static BusinessException forbidden() {
        return new BusinessException(403, "无权限");
    }
}
