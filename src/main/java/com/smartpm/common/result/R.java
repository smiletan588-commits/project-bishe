package com.smartpm.common.result;

import lombok.Data;

@Data
public class R<T> {

    private int code;
    private String msg;
    private T data;

    private R() {}

    // ========== 成功 ==========

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "success";
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(String msg, T data) {
        R<T> r = ok(data);
        r.msg = msg;
        return r;
    }

    // ========== 失败 ==========

    public static <T> R<T> error(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> R<T> error(String msg) {
        return error(500, msg);
    }
}
