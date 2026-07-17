package org.springframework.http;

/**
 * 架构规则测试使用的 Spring HTTP 类型桩。
 *
 * @param <T> 响应体类型。
 */
public class ResponseEntity<T> {

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>();
    }
}
