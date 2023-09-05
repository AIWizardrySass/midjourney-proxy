package com.github.novicezk.midjourney.result;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yubo.ge
 * @date 2023/9/5 22:19
 **/
@Data
public class ResponseObject<T> implements Serializable {
    int code;

    String msg;

    T data;

    public static <T> ResponseObject<T> success() {
        return buildResponse(200, "success", null);
    }


    public static <T> ResponseObject<T> success(T data) {
        return buildResponse(200, "success", data);
    }

    public static ResponseObject<Void> paramInvalid(String msg) {
        return buildResponse(401, msg);
    }

    public static <T> ResponseObject<T> innerError() {
        return innerError("系统异常,请稍后重试");
    }


    public static <T> ResponseObject<T> innerError(String msg) {
        return buildResponse(501, msg);
    }

    private static <T> ResponseObject<T> buildResponse(int code, String msg) {
        return buildResponse(code, msg, null);
    }

    private static <T> ResponseObject<T> buildResponse(int code, String msg, T data) {
        ResponseObject<T> response = new ResponseObject<>();
        response.setCode(code);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }
}
