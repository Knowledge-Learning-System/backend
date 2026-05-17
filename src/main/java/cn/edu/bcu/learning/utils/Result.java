package cn.edu.bcu.learning.utils;

import cn.edu.bcu.learning.enums.ResponseCodeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    private static <T> Result<T> restResult(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    private static <T> Result<T> restResult(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    public static <T> Result<T> success() {
        return restResult(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMsg());
    }

    public static <T> Result<T> success(T data) {
        return restResult(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMsg(), data);
    }

    public static <T> Result<T> failed() {
        return restResult(ResponseCodeEnum.FAILED.getCode(), ResponseCodeEnum.FAILED.getMsg());
    }

    public static <T> Result<T> custom(ResponseCodeEnum responseCode) {
        return restResult(responseCode.getCode(), responseCode.getMsg());
    }
}