package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.core.service.IPaymentOpenApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentOpenApiHttpContractTest {

    private CapturingOpenApiService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = new CapturingOpenApiService();
        mvc = MockMvcBuilders.standaloneSetup(new PaymentOpenApiController(service)).build();
    }

    @Test
    void createOrderBindsBodySignatureHeadersAndCanonicalRequestPath() throws Exception {
        mvc.perform(post("/openapi/pay/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizOrderNo\":\"BIZ-1001\"}")
                        .header("AppId", "app-1")
                        .header("tenantId", "tenant-a")
                        .header("timestamp", "1783900000")
                        .header("nonce", "nonce-1")
                        .header("signature", "signature-1"))
                .andExpect(status().isOk());

        assertThat(service.lastCommand.getBody()).isEqualTo("{\"bizOrderNo\":\"BIZ-1001\"}");
        assertThat(service.lastCommand.getAppId()).isEqualTo("app-1");
        assertThat(service.lastCommand.getTenantId()).isEqualTo("tenant-a");
        assertThat(service.lastCommand.getTimestamp()).isEqualTo("1783900000");
        assertThat(service.lastCommand.getNonce()).isEqualTo("nonce-1");
        assertThat(service.lastCommand.getSignature()).isEqualTo("signature-1");
        assertThat(service.lastCommand.getRequestPath()).isEqualTo("/openapi/pay/orders");
    }

    @Test
    void detailOrderBindsPathAndCompatibilityHeadersAndUsesFirstForwardedAddress() throws Exception {
        mvc.perform(get("/openapi/pay/orders/BIZ-2002")
                        .header("X-Mango-Payment-App-Id", "app-2")
                        .header("X-Mango-Payment-Tenant-Id", "tenant-b")
                        .header("X-Mango-Payment-Timestamp", "1783900001")
                        .header("X-Mango-Payment-Nonce", "nonce-2")
                        .header("X-Mango-Payment-Signature", "signature-2")
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.8"))
                .andExpect(status().isOk());

        assertThat(service.lastCommand.getBizOrderNo()).isEqualTo("BIZ-2002");
        assertThat(service.lastCommand.getAppId()).isEqualTo("app-2");
        assertThat(service.lastCommand.getTenantId()).isEqualTo("tenant-b");
        assertThat(service.lastCommand.getRequestPath()).isEqualTo("/openapi/pay/orders/BIZ-2002");
        assertThat(service.lastCommand.getClientIp()).isEqualTo("203.0.113.7");
    }

    private static final class CapturingOpenApiService implements IPaymentOpenApiService {

        private PaymentOpenRequestCommand lastCommand;

        @Override
        public R<PaymentOpenBusinessOrderVO> createOrder(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenBusinessOrderVO());
        }

        @Override
        public R<PaymentOpenBusinessOrderVO> detailOrder(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenBusinessOrderVO());
        }

        @Override
        public R<PaymentOpenCashierVO> cashier(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenCashierVO());
        }

        @Override
        public R<PaymentOpenPaymentOrderVO> pay(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenPaymentOrderVO());
        }

        @Override
        public R<PaymentOpenPaymentOrderVO> detailPaymentOrder(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenPaymentOrderVO());
        }

        @Override
        public R<PaymentOpenRefundOrderVO> refund(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenRefundOrderVO());
        }

        @Override
        public R<PaymentOpenRefundOrderVO> detailRefund(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenRefundOrderVO());
        }

        @Override
        public R<PaymentOpenReceiptVO> receipt(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return R.ok(new PaymentOpenReceiptVO());
        }
    }
}
