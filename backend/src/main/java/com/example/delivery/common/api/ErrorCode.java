package com.example.delivery.common.api;

public enum ErrorCode {
    PARAM_ERROR(400001, "请求参数错误"),
    UNAUTHORIZED(401001, "未登录或 Token 失效"),
    FORBIDDEN(403001, "无操作权限"),
    NOT_FOUND(404001, "数据不存在"),
    STATE_CONFLICT(409001, "数据状态冲突"),
    INTERNAL_ERROR(500001, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
