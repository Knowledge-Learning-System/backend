package cn.edu.bcu.learning.enums;

import lombok.Getter;

@Getter
public enum ResponseCodeEnum {

    SUCCESS(0, "成功"),
    FAILED(1001, "系统内部错误"),
    PARAMETER_NULL(1002, "参数为空"),
    NOT_FOUND(1003, "资源不存在"),
    AUTHENTICATION_FAILED(1004, "认证失败");

    private final Integer code;
    private final String msg;

    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}