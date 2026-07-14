package io.mango.payment.core.integration;

/**
 * 支付域调用远程 API 后的边界结果。
 *
 * @param success 是否成功。
 * @param data 返回数据。
 * @param message 返回消息。
 * @param <T> 数据类型。
 */
public record PaymentRemoteOutcome<T>(boolean success, T data, String message) {}
