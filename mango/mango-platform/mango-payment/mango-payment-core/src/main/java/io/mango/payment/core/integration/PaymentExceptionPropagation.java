package io.mango.payment.core.integration;

/** 保留远端或事务异常类型与实例的传播边界。 */
public final class PaymentExceptionPropagation {

    private PaymentExceptionPropagation() {
    }

    public static <T> T rethrow(RuntimeException exception) {
        throw exception;
    }
}
