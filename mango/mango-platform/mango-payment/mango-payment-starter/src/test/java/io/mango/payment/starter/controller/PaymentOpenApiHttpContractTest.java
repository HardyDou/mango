package io.mango.payment.starter.controller;

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
    void createOrderBindsCommandAndOverwritesCanonicalRequestPath() throws Exception {
        mvc.perform(post("/openapi/pay/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"{\\"bizOrderNo\\":\\"BIZ-1001\\"}","appId":"app-1",
                                "tenantId":"tenant-a","timestamp":"1783900000","nonce":"nonce-1",
                                "signature":"signature-1","requestPath":"/spoofed","bizOrderNo":"BIZ-1001"}
                                """)
                        .header("X-Real-IP", "198.51.100.8"))
                .andExpect(status().isOk());

        assertThat(service.lastCommand.getBody()).isEqualTo("{\"bizOrderNo\":\"BIZ-1001\"}");
        assertThat(service.lastCommand.getAppId()).isEqualTo("app-1");
        assertThat(service.lastCommand.getTenantId()).isEqualTo("tenant-a");
        assertThat(service.lastCommand.getTimestamp()).isEqualTo("1783900000");
        assertThat(service.lastCommand.getNonce()).isEqualTo("nonce-1");
        assertThat(service.lastCommand.getSignature()).isEqualTo("signature-1");
        assertThat(service.lastCommand.getRequestPath()).isEqualTo("/openapi/pay/orders/create");
        assertThat(service.lastCommand.getClientIp()).isEqualTo("198.51.100.8");
    }

    @Test
    void detailOrderBindsFixedRouteCommandAndUsesFirstForwardedAddress() throws Exception {
        mvc.perform(post("/openapi/pay/orders/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"appId":"app-2","tenantId":"tenant-b","timestamp":"1783900001",
                                "nonce":"nonce-2","signature":"signature-2","bizOrderNo":"BIZ-2002"}
                                """)
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.8"))
                .andExpect(status().isOk());

        assertThat(service.lastCommand.getBizOrderNo()).isEqualTo("BIZ-2002");
        assertThat(service.lastCommand.getAppId()).isEqualTo("app-2");
        assertThat(service.lastCommand.getTenantId()).isEqualTo("tenant-b");
        assertThat(service.lastCommand.getRequestPath()).isEqualTo("/openapi/pay/orders/detail");
        assertThat(service.lastCommand.getClientIp()).isEqualTo("203.0.113.7");
    }

    private static final class CapturingOpenApiService implements IPaymentOpenApiService {

        private PaymentOpenRequestCommand lastCommand;

        @Override
        public PaymentOpenBusinessOrderVO createOrder(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenBusinessOrderVO();
        }

        @Override
        public PaymentOpenBusinessOrderVO detailOrder(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenBusinessOrderVO();
        }

        @Override
        public PaymentOpenCashierVO cashier(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenCashierVO();
        }

        @Override
        public PaymentOpenPaymentOrderVO pay(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenPaymentOrderVO();
        }

        @Override
        public PaymentOpenPaymentOrderVO detailPaymentOrder(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenPaymentOrderVO();
        }

        @Override
        public PaymentOpenRefundOrderVO refund(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenRefundOrderVO();
        }

        @Override
        public PaymentOpenRefundOrderVO detailRefund(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenRefundOrderVO();
        }

        @Override
        public PaymentOpenReceiptVO receipt(PaymentOpenRequestCommand command) {
            lastCommand = command;
            return new PaymentOpenReceiptVO();
        }
    }
}
