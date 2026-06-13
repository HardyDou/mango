package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRefundApprovalMapperContractTest {

    @Test
    @DisplayName("payment order view should count pending and in-approval refunds as occupied amount")
    void paymentOrderView_shouldCountWorkflowApprovalsAsOccupiedRefundAmount() throws IOException {
        String paymentOrderMapper = resource("/mapper/payment/PaymentOrderMapper.xml");

        assertThat(paymentOrderMapper)
                .contains("approval.status in ('PENDING', 'IN_APPROVAL')");
    }

    private String resource(String path) throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
