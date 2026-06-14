package io.mango.payment.core.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceDependencyCycleContractTest {

    @Test
    void paymentExceptionRecordWritersShouldNotDependOnExceptionOrderHandlingService() {
        assertDoesNotDeclareDependency(PaymentDuplicatePaymentService.class, PaymentExceptionOrderService.class);
        assertDoesNotDeclareDependency(PaymentChannelSyncService.class, PaymentExceptionOrderService.class);
        assertDoesNotDeclareDependency(PaymentChannelCallbackService.class, PaymentExceptionOrderService.class);
        assertDoesNotDeclareDependency(PaymentChannelOrderCloseService.class, PaymentExceptionOrderService.class);
    }

    @Test
    void paymentExceptionRecordWritersShouldUseRecordService() {
        assertDeclaresDependency(PaymentDuplicatePaymentService.class, PaymentExceptionOrderRecordService.class);
        assertDeclaresDependency(PaymentChannelSyncService.class, PaymentExceptionOrderRecordService.class);
        assertDeclaresDependency(PaymentChannelCallbackService.class, PaymentExceptionOrderRecordService.class);
        assertDeclaresDependency(PaymentChannelOrderCloseService.class, PaymentExceptionOrderRecordService.class);
    }

    @Test
    void paymentExceptionHandlingServiceMayCoordinatePaymentActions() {
        assertDeclaresDependency(PaymentExceptionOrderService.class, PaymentChannelSyncService.class);
        assertDeclaresDependency(PaymentExceptionOrderService.class, PaymentChannelOrderCloseService.class);
        assertDeclaresDependency(PaymentExceptionOrderService.class, PaymentExceptionOrderRecordService.class);
    }

    private static void assertDoesNotDeclareDependency(Class<?> source, Class<?> dependency) {
        assertFalse(declaresDependency(source, dependency),
                source.getSimpleName() + " must not depend on " + dependency.getSimpleName());
    }

    private static void assertDeclaresDependency(Class<?> source, Class<?> dependency) {
        assertTrue(declaresDependency(source, dependency),
                source.getSimpleName() + " must depend on " + dependency.getSimpleName());
    }

    private static boolean declaresDependency(Class<?> source, Class<?> dependency) {
        return Arrays.stream(source.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(dependency::equals);
    }
}
