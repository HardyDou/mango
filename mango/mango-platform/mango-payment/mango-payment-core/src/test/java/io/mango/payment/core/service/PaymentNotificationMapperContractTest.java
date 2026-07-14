package io.mango.payment.core.service;

import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentNotificationMapperContractTest {

    @Test
    void dueNotificationTenantIdsUseStringAcrossJavaAndMyBatisContracts() throws Exception {
        Method method = PaymentNotificationRecordMapper.class.getMethod(
                "selectDueNotificationTenantIds", java.time.LocalDateTime.class, long.class);
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments()).containsExactly(String.class);
        assertThat(resource("/mapper/payment/PaymentNotificationRecordMapper.xml"))
                .contains("id=\"selectDueNotificationTenantIds\" resultType=\"string\"");
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getResourceAsStream(path)) {
            assertThat(input).as("resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
