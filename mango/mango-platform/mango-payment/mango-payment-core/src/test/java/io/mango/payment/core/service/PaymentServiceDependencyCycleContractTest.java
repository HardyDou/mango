package io.mango.payment.core.service;

import io.mango.payment.core.service.impl.PaymentChannelCallbackService;
import io.mango.payment.core.service.impl.PaymentExceptionOrderService;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceDependencyCycleContractTest {

    @Test
    void paymentExceptionRecordWritersShouldNotDependOnExceptionOrderHandlingService() {
        assertDoesNotDeclareDependency(PaymentDuplicatePaymentGuard.class, PaymentExceptionOrderService.class);
        assertDoesNotDeclareDependency(PaymentChannelSynchronizer.class, PaymentExceptionOrderService.class);
        assertDoesNotDeclareDependency(PaymentChannelCallbackService.class, PaymentExceptionOrderService.class);
        assertDoesNotDeclareDependency(PaymentChannelOrderCloseCoordinator.class, PaymentExceptionOrderService.class);
    }

    @Test
    void paymentExceptionRecordWritersShouldUseRecordService() {
        assertDeclaresDependency(PaymentDuplicatePaymentGuard.class, PaymentExceptionOrderRecorder.class);
        assertDeclaresDependency(PaymentChannelSynchronizer.class, PaymentExceptionOrderRecorder.class);
        assertDeclaresDependency(PaymentChannelCallbackService.class, PaymentExceptionOrderRecorder.class);
        assertDeclaresDependency(PaymentChannelOrderCloseCoordinator.class, PaymentExceptionOrderRecorder.class);
    }

    @Test
    void paymentExceptionHandlingServiceMayCoordinatePaymentActions() {
        assertDeclaresDependency(PaymentExceptionOrderService.class, PaymentChannelSynchronizer.class);
        assertDeclaresDependency(PaymentExceptionOrderService.class, PaymentChannelOrderCloseCoordinator.class);
        assertDeclaresDependency(PaymentExceptionOrderService.class, PaymentExceptionOrderRecorder.class);
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
